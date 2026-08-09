package com.smartpark.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartpark.pojo.entity.enterprise.EnterpriseTypeCheck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EnterpriseTypeCheckMapper extends BaseMapper<EnterpriseTypeCheck> {

    @Select("SELECT etc.* FROM enterprise_type_check etc " +
            "INNER JOIN check_item_def cid ON cid.id = etc.check_item_id " +
            "WHERE etc.type_id = #{typeId} AND etc.status = 'ENABLED' AND cid.status = 'ENABLED' " +
            "ORDER BY etc.step_order ASC")
    List<EnterpriseTypeCheck> selectEnabledByTypeId(@Param("typeId") Long typeId);

    @Select("SELECT COUNT(1) FROM enterprise_type_check " +
            "WHERE type_id = #{typeId} AND status = 'ENABLED'")
    Integer countStepsByTypeId(@Param("typeId") Long typeId);
}
