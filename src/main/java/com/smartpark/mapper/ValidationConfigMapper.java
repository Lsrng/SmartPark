package com.smartpark.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartpark.pojo.entity.ValidationConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ValidationConfigMapper extends BaseMapper<ValidationConfig> {

    List<ValidationConfig> selectByTypeAndVersion(
            @Param("enterpriseTypeId") Long enterpriseTypeId,
            @Param("configVersion") Integer configVersion);

    List<ValidationConfig> selectAllEnabled();
}
