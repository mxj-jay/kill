package com.debug.kill.server.service;

/**
 * @author 59742
 */
public interface KillService {

    Boolean killItem(Integer killId,Integer userId) throws Exception;

    /**数据库层优化抢单逻辑*/
    Boolean killItemV2(Integer killId,Integer userId) throws Exception;

    /**redis实现分布式锁*/
    Boolean killItemV3(Integer killId, Integer userId) throws Exception;

    /**redisson的分布式锁*/
    Boolean killItemV4(Integer killId, Integer userId) throws Exception;

    /**rabbitmq实现分布式锁*/
    Boolean killItemV5(Integer killId, Integer userId) throws Exception;

}
