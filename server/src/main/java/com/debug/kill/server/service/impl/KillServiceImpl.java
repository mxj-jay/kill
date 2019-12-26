package com.debug.kill.server.service.impl;

import com.debug.kill.model.entity.ItemKill;
import com.debug.kill.model.entity.ItemKillSuccess;
import com.debug.kill.model.mapper.ItemKillMapper;
import com.debug.kill.model.mapper.ItemKillSuccessMapper;
import com.debug.kill.server.enums.SysConstant;
import com.debug.kill.server.service.KillService;
import com.debug.kill.server.utils.SnowFlake;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        }

    }
}
