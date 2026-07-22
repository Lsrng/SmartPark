package com.smartpark.service.Impl;

import com.alibaba.fastjson.JSON;
import com.smartpark.common.annotation.RateLimit;
import com.smartpark.mapper.DeviceDataMapper;
import com.smartpark.pojo.dto.DeviceDataReportDTO;
import com.smartpark.pojo.entity.DeviceDataRecord;
import com.smartpark.service.DeviceDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 物联设备数据上报服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceDataServiceImpl implements DeviceDataService {

    private final DeviceDataMapper deviceDataMapper;

    @Override
    @RateLimit(
            rate = 1,
            rateInterval = 60,
            timeUnit = com.smartpark.common.annotation.RateIntervalUnit.SECONDS,
            keyField = "#dto.deviceId"
    )
    public void report(DeviceDataReportDTO dto) {
        // 构建实体
        DeviceDataRecord record = DeviceDataRecord.builder()
                .deviceId(dto.getDeviceId())
                .data(JSON.toJSONString(dto.getData()))
                .reportTime(dto.getReportTime() != null ? dto.getReportTime() : LocalDateTime.now())
                .build();

        // 写入数据库
        deviceDataMapper.insert(record);

        log.info("设备数据上报成功 - 设备ID: {}, 上报时间: {}", record.getDeviceId(), record.getReportTime());
    }
}
