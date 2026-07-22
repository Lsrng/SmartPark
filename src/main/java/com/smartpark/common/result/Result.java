package com.smartpark.common.result;

import com.smartpark.common.enumeration.HttpStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 后端统一返回结果封装类
 * @param <T> 泛型数据类型
 */
@Data
@Schema(description = "统一响应体")
public class Result<T> implements Serializable {

    @Schema(description = "状态码，如200表示成功，400表示参数错误")
    private Integer code;

    @Schema(description = "提示信息")
    private String msg;

    @Schema(description = "响应数据")
    private T data;

    /** 成功无数据 */
    public static <T> Result<T> success() {
        return build(HttpStatusEnum.SUCCESS.getCode(), HttpStatusEnum.SUCCESS.getMessage(), null);
    }

    /** 成功返回数据 */
    public static <T> Result<T> success(T data) {
        return build(HttpStatusEnum.SUCCESS.getCode(), HttpStatusEnum.SUCCESS.getMessage(), data);
    }

    /** 使用自定义成功提示 - 有数据返回 */
    public static <T> Result<T> success(String msg, T data) {
        return build(HttpStatusEnum.SUCCESS.getCode(), msg, data);
    }

    /** 使用自定义成功提示 - 无数据返回*/
    public static <T> Result<T> success(String msg) {
        return build(HttpStatusEnum.SUCCESS.getCode(), msg,null);
    }

    /**
     * 成功响应，无数据返回（状态枚举）
     *
     * @param statusEnum
     * @return
     * @param <T>
     */
    public static <T> Result<T> success(HttpStatusEnum statusEnum) {
        return build(statusEnum.getCode(), statusEnum.getMessage(), null);
    }

    /**
     * 成功响应，有数据返回（状态枚举 + 返回数据）
     *
     * @param statusEnum
     * @param data
     * @return
     * @param <T>
     */
    public static <T> Result<T> success(HttpStatusEnum statusEnum,T data) {
        return build(statusEnum.getCode(), statusEnum.getMessage(), data);
    }

    /**
     * 成功响应，无数据返回（状态枚举 + 自定义提示信息）
     *
     *<p>可自定义提示信息</p>
     *
     * @param statusEnum
     * @param message
     * @return
     * @param <T>
     */
    public static <T> Result<T> success(HttpStatusEnum statusEnum,String message) {
        return build(statusEnum.getCode(), message, null);
    }

    /**
     * 成功响应，有数据返回（状态枚举 + 自定义提示信息）
     *
     *<p>可自定义提示信息</p>
     *
     * @param statusEnum
     * @param message
     * @param data
     * @return
     * @param <T>
     */
    public static <T> Result<T> success(HttpStatusEnum statusEnum,String message,T data) {
        return build(statusEnum.getCode(), message, data);
    }

    /** 自定义错误信息 */
    public static <T> Result<T> error(String msg) {
        return build(HttpStatusEnum.ERROR.getCode(), msg, null);
    }

    /** 自定义错误码和信息 */
    public static <T> Result<T> error(int code, String msg) {
        return build(code, msg, null);
    }

    /** 使用枚举返回失败信息 */
    public static <T> Result<T> error(HttpStatusEnum statusEnum) {
        return build(statusEnum.getCode(), statusEnum.getMessage(), null);
    }

    /**
     * 自定义错误码和信息
     *
     * @param statusEnum
     * @param message
     * @return
     * @param <T>
     */
    public static <T> Result<T> error(HttpStatusEnum statusEnum,String message) {
        return build(statusEnum.getCode(), message, null);
    }


    /** 构建通用返回体 */
    private static <T> Result<T> build(int code, String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }
}
