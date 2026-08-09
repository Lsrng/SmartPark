package com.smartpark.upload.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartpark.upload.pojo.entity.FileMetadata;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {
}
