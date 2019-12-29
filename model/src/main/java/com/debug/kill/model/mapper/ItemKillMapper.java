package com.debug.kill.model.mapper;

import com.debug.kill.model.entity.ItemKill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Damocles
 */
@Mapper
public interface ItemKillMapper {
    List<ItemKill> selectAll();

    ItemKill selectById(@Param("id") Integer id);

    int updateKillItem(@Param("killId") Integer killId);


    /**数据库层优化抢单逻辑
     * 保证秒杀商品的总数始终大于0, 即total>0
     * 当并发线程数很大时仍然存在同时抢单
     * */
    ItemKill selectByIdV2(@Param("id") Integer id);

    int updateKillItemV2(@Param("killId") Integer killId);
}