package com.smartpark.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartpark.pojo.entity.PropertyBill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PropertyBillMapper extends BaseMapper<PropertyBill> {

    int batchInsert(@Param("list") List<PropertyBill> list);
}
