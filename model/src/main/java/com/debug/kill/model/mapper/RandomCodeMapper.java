package com.debug.kill.model.mapper;

import com.debug.kill.model.entity.RandomCode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RandomCodeMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(RandomCode record);

    int insertSelective(RandomCode record);

    RandomCode selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(RandomCode record);

    int updateByPrimaryKey(RandomCode record);
}