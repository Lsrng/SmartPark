package com.smartpark.common.enumeration;

import lombok.Getter;

/**
 * 常用HTTP状态返回枚举
 */
@Getter
public enum HttpStatusEnum {

    /**
     * 200 OK：请求成功，服务器成功返回所请求的数据
     */
    SUCCESS(200, "操作成功"),

    /**
     * 201 Created：请求成功并创建了新资源（如新增数据）
     */
    CREATED(201, "对象创建成功"),

    /**
     * 204 No Content：请求成功，但响应体为空（用于删除、修改操作）
     */
    NO_CONTENT(204, "操作成功，无返回数据"),

    /**
     * 400 Bad Request：客户端请求参数错误（缺参数、类型错误等）
     */
    BAD_REQUEST(400, "请求参数错误"),

    /**
     * 401 Unauthorized：请求未认证或认证失败（如未登录、token过期）
     */
    UNAUTHORIZED(401, "未授权"),

    /**
     * 403 Forbidden：请求已认证，但无权限访问
     */
    FORBIDDEN(403, "无访问权限"),

    /**
     * 404 Not Found：请求的资源不存在（路径或接口不存在）
     */
    NOT_FOUND(404, "资源未找到"),

    /**
     * 409 Conflict：请求资源存在冲突（如用户名重复、数据版本冲突）
     */
    CONFLICT(409, "资源冲突"),

    /**
     * 500 Internal Server Error：服务端程序出错
     */
    ERROR(500, "服务器内部错误"),

    /**
     * 429 Too Many Requests：请求频率过高，被限流
     */
    TOO_MANY_REQUESTS(429, "请求频率过高，请稍后重试"),

    /**
     * 501 Not Implemented：请求方法未被实现（占位接口或未开发）
     */
    NOT_IMPLEMENTED(501, "接口未实现");

    private final Integer code;
    private final String message;

    HttpStatusEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
