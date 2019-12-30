package com.debug.kill.server.service.impl;

import com.debug.kill.model.entity.ItemKill;
import com.debug.kill.model.entity.ItemKillSuccess;
import com.debug.kill.model.mapper.ItemKillMapper;
import com.debug.kill.model.mapper.ItemKillSuccessMapper;
import com.debug.kill.server.enums.SysConstant;
import com.debug.kill.server.service.KillService;
import com.debug.kill.server.service.RabbitSenderService;
import com.debug.kill.server.utils.RandomUtil;
import com.debug.kill.server.utils.SnowFlake;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.joda.time.DateTime;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author 59742
 */
@Service
public class KillServiceImpl implements KillService {

    private static final Logger logger = LoggerFactory.getLogger(KillServiceImpl.class);

    private SnowFlake snowFlake = new SnowFlake(2,3);


    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    @Autowired
    private ItemKillMapper itemKillMapper;

    @Autowired
    private RabbitSenderService rabbitSenderService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CuratorFramework curatorFramework;

    private static final String pathPrefix="/kill/zkLock/";


    @Override
    public Boolean killItem(Integer killId, Integer userId) throws Exception {
        Boolean res = false;

        //TODO: 首先判断当前用户是否已经抢过该商品
        if (itemKillSuccessMapper.countByKillUserId(killId, userId) <=0){
            //TODO: 查询待秒杀商品的详情
            ItemKill itemKill = itemKillMapper.selectById(killId);

            //TODO：判断当前商品是否可以被秒杀，即cankill=1？
            if (itemKill!=null && 1==itemKill.getCanKill()){
                int count = itemKillMapper.updateKillItem(killId);
                //TODO:扣减是否成功?是-生成秒杀成功的订单，同时通知用户秒杀成功的消息
                if (count>0){
                    commonRecordKillSuccessInfo(itemKill,userId);
                    res = true;
                }
            }
        }else {
            throw new Exception("您已经抢购过该商品了!");
        }

        return res;
    }

    /**
     * 商品秒杀核心业务逻辑的处理-mysql的优化
     * @param killId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public Boolean killItemV2(Integer killId, Integer userId) throws Exception {
        Boolean result=false;

        //TODO:判断当前用户是否已经抢购过当前商品
        if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){
            //TODO:A.查询待秒杀商品详情
            ItemKill itemKill=itemKillMapper.selectByIdV2(killId);

            //TODO:判断是否可以被秒杀canKill=1?
            if (itemKill!=null && 1==itemKill.getCanKill() && itemKill.getTotal()>0){
                //TODO:B.扣减库存-减一
                int res=itemKillMapper.updateKillItemV2(killId);

                //TODO:扣减是否成功?是-生成秒杀成功的订单，同时通知用户秒杀成功的消息
                if (res>0){
                    commonRecordKillSuccessInfo(itemKill,userId);

                    result=true;
                }
            }
        }else{
            throw new Exception("您已经抢购过该商品了!");
        }
        return result;
    }

    /**
     * 商品秒杀核心业务逻辑的处理-redis的分布式锁
     * @param killId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public Boolean killItemV3(Integer killId, Integer userId) throws Exception {
        Boolean result=false;

        if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){

            //TODO:借助Redis的原子操作实现分布式锁-对共享操作-资源进行控制
            ValueOperations valueOperations=stringRedisTemplate.opsForValue();
            final String key=new StringBuffer().append(killId).append(userId).append("-RedisLock").toString();
            final String value= RandomUtil.generateOrderCode();
            //luna脚本提供“分布式锁服务”，就可以写在一起
            Boolean cacheRes=valueOperations.setIfAbsent(key,value);
            //TOOD:redis部署节点宕机了
            if (cacheRes){
                stringRedisTemplate.expire(key,30, TimeUnit.SECONDS);

                try {
                    ItemKill itemKill=itemKillMapper.selectByIdV2(killId);
                    if (itemKill!=null && 1==itemKill.getCanKill() && itemKill.getTotal()>0){
                        int res=itemKillMapper.updateKillItemV2(killId);
                        if (res>0){
                            commonRecordKillSuccessInfo(itemKill,userId);

                            result=true;
                        }
                    }
                }catch (Exception e){
                    throw new Exception("还没到抢购日期、已过了抢购时间或已被抢购完毕！");
                }finally {
                    if (value.equals(valueOperations.get(key).toString())){
                        stringRedisTemplate.delete(key);
                    }
                }
            }
        }else{
            throw new Exception("Redis-您已经抢购过该商品了!");
        }
        return result;
    }

    /**
     * 商品秒杀核心业务逻辑的处理-redisson的分布式锁
     * @param killId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public Boolean killItemV4(Integer killId, Integer userId) throws Exception {
        Boolean result = false;

        final String lockKey=new StringBuffer().append(killId).append(userId).append("-RedissonLock").toString();
        RLock lock = redissonClient.getLock(lockKey);

        try{
            boolean isLock = lock.tryLock(30, 10, TimeUnit.SECONDS);
            if (isLock) {
                //TODO:判断当前用户是否已经抢购过当前商品
                if (itemKillSuccessMapper.countByKillUserId(killId, userId) <= 0) {
                    //TODO:A.查询待秒杀商品详情
                    ItemKill itemKill = itemKillMapper.selectByIdV2(killId);
                    //TODO:判断是否可以被秒杀canKill=1?
                    if (itemKill != null && 1 == itemKill.getCanKill() && itemKill.getTotal() > 0) {
                        //TODO:B.扣减库存-减一
                        int res = itemKillMapper.updateKillItemV2(killId);

                        //TODO:扣减是否成功?是-生成秒杀成功的订单，同时通知用户秒杀成功的消息
                        if (res > 0) {
                            commonRecordKillSuccessInfo(itemKill, userId);

                            result = true;
                        }
                    }
                } else {
                    throw new Exception("您已经抢购过该商品了!");
                }
            }
        }finally {
            lock.unlock();
        }
        return result;
    }

    /**
     * 商品秒杀核心业务逻辑的处理-zookeeper实现分布式锁
     * @param killId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public Boolean killItemV5(Integer killId, Integer userId) throws Exception {
        Boolean result = false;

        InterProcessMutex interProcessMutex = new InterProcessMutex(curatorFramework, pathPrefix + killId + userId + "-lock");

        try{
            if (interProcessMutex.acquire(10,TimeUnit.SECONDS)) {
                if (itemKillSuccessMapper.countByKillUserId(killId, userId) <= 0) {
                    ItemKill itemKill = itemKillMapper.selectByIdV2(killId);
                    if (itemKill != null && 1 == itemKill.getCanKill() && itemKill.getTotal() > 0) {
                        int res = itemKillMapper.updateKillItemV2(killId);
                        if (res > 0) {
                            commonRecordKillSuccessInfo(itemKill, userId);
                            result = true;
                        }
                    }
                } else {
                    throw new Exception("您已经抢购过该商品了!");
                }
            }
        }finally {
            interProcessMutex.release();
        }
        return result;
    }

    /**
     * 公共方法 -- 记录用户秒杀成功后生成订单 - 并且异步邮件通知
     * @param kill
     * @param userId
     */
    public void commonRecordKillSuccessInfo(ItemKill kill, Integer userId){
        //TODO: 记录用户秒杀成功后生成订单
        ItemKillSuccess itemKillSuccess = new ItemKillSuccess();
        // 雪花算法生成流水号
        String orderNo = String.valueOf(snowFlake.nextId());

        itemKillSuccess.setCode(orderNo);
        itemKillSuccess.setItemId(kill.getItemId());
        itemKillSuccess.setKillId(kill.getId());
        itemKillSuccess.setUserId(userId.toString());
        itemKillSuccess.setStatus(SysConstant.OrderStatus.SuccessNotPayed.getCode().byteValue());
        itemKillSuccess.setCreateTime(DateTime.now().toDate());

        //TODO：入库
        if (itemKillSuccessMapper.countByKillUserId(kill.getId(),userId) <= 0){
            itemKillSuccessMapper.insert(itemKillSuccess);

            //TODO: 异步发送邮件信息 rabbitmq + javamail
            rabbitSenderService.sendKillSuccessEmailMsg(orderNo);

            //TODO: 入死信队列, 超时秒杀未支付更改状态
            rabbitSenderService.sendKillSuccessOrderExpireMsg(orderNo);

        }

    }
}
