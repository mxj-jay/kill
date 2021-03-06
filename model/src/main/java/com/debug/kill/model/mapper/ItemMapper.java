package com.debug.kill.model.mapper;

import com.debug.kill.model.entity.Item;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ItemMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Item record);

    int insertSelective(Item record);

    Item selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Item record);

    int updateByPrimaryKey(Item record);
}