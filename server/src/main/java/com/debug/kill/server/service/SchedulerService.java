package com.debug.kill.server.service;

import com.debug.kill.model.entity.ItemKillSuccess;
import com.debug.kill.model.mapper.ItemKillSuccessMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

/**
 * 定时任务服务
 *
 * @author Damocles
 */
@Service
public class SchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    @Autowired
    private Environment env;

    /**
     * 定时获取status=0的订单并判断是否超过TTL，然后进行失效
     */
    @Scheduled(cron = "0/10 * * * * ? ")
    public void schedulerExpireOrders() {
        logger.info("-----定时任务1-----");

        try {
            List<ItemKillSuccess> list = itemKillSuccessMapper.selectExpireOrders();
            if (list != null && !list.isEmpty()) {
                // java8 循环写法
                list.stream().forEach(new Consumer<ItemKillSuccess>() {
                    @Override
                    public void accept(ItemKillSuccess itemKillSuccess) {
                        if (itemKillSuccess != null && itemKillSuccess.getDiffTime() > env.getProperty("scheduler.expire.orders.time", Integer.class)) {
                            itemKillSuccessMapper.expireOrder(itemKillSuccess.getCode());
                        }
                    }
                });
            }

        } catch (Exception e) {
            logger.error("定时获取status=0的订单并判断是否超过TTL，然后进行失效-发生异常：", e.fillInStackTrace());
        }

    }

    @Scheduled(cron = "0/10 * * * * ?")
    public void schedulerExpireOrdersV2(){
        logger.info("v2的定时任务----");
    }

    @Scheduled(cron = "0/10 * * * * ?")
    public void schedulerExpireOrdersV3(){
        logger.info("v3的定时任务----");
    }

}
