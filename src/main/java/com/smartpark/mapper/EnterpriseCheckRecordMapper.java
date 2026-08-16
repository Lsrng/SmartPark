package com.smartpark.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartpark.pojo.entity.EnterpriseCheckRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface EnterpriseCheckRecordMapper extends BaseMapper<EnterpriseCheckRecord> {

    List<EnterpriseCheckRecord> selectByRegisterId(@Param("registerId") Long registerId);

    boolean isStepPassed(@Param("registerId") Long registerId, @Param("stepOrder") Integer stepOrder);

    @Update("UPDATE enterprise_check_record SET status = 'PENDING', updated_at = NOW() " +
            "WHERE register_id = #{registerId} AND step_order > #{stepOrder}")
    int resetStepsAfter(@Param("registerId") Long registerId, @Param("stepOrder") Integer stepOrder);
}
