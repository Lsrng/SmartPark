package com.smartpark.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartpark.pojo.entity.enterprise.EnterpriseCheckRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface EnterpriseCheckRecordMapper extends BaseMapper<EnterpriseCheckRecord> {

    @Select("SELECT * FROM enterprise_check_record WHERE register_id = #{registerId} ORDER BY step_order ASC")
    List<EnterpriseCheckRecord> selectByRegisterId(@Param("registerId") Long registerId);

    @Select("SELECT * FROM enterprise_check_record WHERE register_id = #{registerId} AND step_order = #{stepOrder}")
    EnterpriseCheckRecord selectByRegisterIdAndStep(@Param("registerId") Long registerId, @Param("stepOrder") Integer stepOrder);

    @Select("SELECT * FROM enterprise_check_record WHERE register_id = #{registerId} AND step_order = #{stepOrder} AND check_status = 'PASSED'")
    EnterpriseCheckRecord selectPassedStep(@Param("registerId") Long registerId, @Param("stepOrder") Integer stepOrder);

    @Update("UPDATE enterprise_check_record SET check_status = 'PENDING', check_result = NULL, checked_by = NULL, checked_at = NULL " +
            "WHERE register_id = #{registerId} AND step_order >= #{fromStep}")
    void resetFromStep(@Param("registerId") Long registerId, @Param("fromStep") Integer fromStep);
}
