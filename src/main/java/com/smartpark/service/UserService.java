package com.smartpark.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartpark.pojo.dto.LoginDTO;
import com.smartpark.pojo.entity.User;

import java.util.Map;

public interface UserService extends IService<User> {

    /**
     * 用户登录
     * @param loginDTO 登录参数
     * @return token等信息
     */
    Map<String, Object> login(LoginDTO loginDTO);
}
