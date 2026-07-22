package com.smartpark.service;

import com.smartpark.pojo.dto.DeviceDataReportDTO;

/**
 * 物联设备数据上报服务接口
 */
public interface DeviceDataService {

    /**
     * 上报设备数据
     *
     * @param dto 设备上报数据
     */
    void report(DeviceDataReportDTO dto);
}
