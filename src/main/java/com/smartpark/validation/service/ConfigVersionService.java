package com.smartpark.validation.service;

import com.smartpark.mapper.EnterpriseConfigVersionMapper;
import com.smartpark.pojo.entity.EnterpriseConfigVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigVersionService {

    private final EnterpriseConfigVersionMapper versionMapper;

    public int nextVersion(Long typeId) {
        EnterpriseConfigVersion version = EnterpriseConfigVersion.builder()
                .typeId(typeId)
                .currentVersion(1)
                .build();
        versionMapper.insert(version);
        return versionMapper.selectCurrentVersion(typeId);
    }

    public int currentVersion(Long typeId) {
        Integer version = versionMapper.selectCurrentVersion(typeId);
        return version != null ? version : 0;
    }

    public void setVersion(Long typeId, int version) {
        versionMapper.updateVersion(typeId, version);
    }
}
