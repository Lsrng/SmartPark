package com.smartpark.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartpark.pojo.entity.EnterpriseConfigVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EnterpriseConfigVersionMapper extends BaseMapper<EnterpriseConfigVersion> {

    @Update("INSERT INTO enterprise_config_version (type_id, current_version) " +
            "VALUES (#{typeId}, 1) " +
            "ON DUPLICATE KEY UPDATE current_version = current_version + 1, updated_at = NOW()")
    int incrementVersion(@Param("typeId") Long typeId);

    Integer selectCurrentVersion(@Param("typeId") Long typeId);

    int updateVersion(@Param("typeId") Long typeId, @Param("version") Integer version);
}
