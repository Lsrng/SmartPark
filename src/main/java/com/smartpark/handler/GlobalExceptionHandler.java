package com.smartpark.handler;

import com.smartpark.common.exception.EnterpriseCheckException;
import com.smartpark.common.exception.RateLimitException;
import com.smartpark.common.result.Result;
import com.smartpark.upload.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理限流异常，返回 HTTP 429
     */
    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Result<Void> handleRateLimitException(RateLimitException e) {
        log.warn("限流拦截 - 设备ID: {}, 原因: {}", e.getDeviceId(), e.getMessage());
        return Result.error(429, e.getMessage());
    }

    /**
     * 处理企业入驻校验异常
     */
    @ExceptionHandler(EnterpriseCheckException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleEnterpriseCheckException(EnterpriseCheckException e) {
        log.warn("入驻校验失败: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 处理文件上传异常体系
     */
    @ExceptionHandler(FileEmptyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleFileEmptyException(FileEmptyException e) {
        log.warn("文件上传失败（空文件）: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(FileSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleFileSizeExceededException(FileSizeExceededException e) {
        log.warn("文件上传失败（大小超限）: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(InvalidFilenameException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleInvalidFilenameException(InvalidFilenameException e) {
        log.warn("文件上传失败（文件名非法）: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(ExtensionNotAllowedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleExtensionNotAllowedException(ExtensionNotAllowedException e) {
        log.warn("文件上传失败（扩展名不在白名单）: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(MagicMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMagicMismatchException(MagicMismatchException e) {
        log.warn("文件上传失败（魔数校验失败）: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(FileTypeUnknownException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleFileTypeUnknownException(FileTypeUnknownException e) {
        log.warn("文件上传失败（类型未知）: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(FileStorageException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleFileStorageException(FileStorageException e) {
        log.error("文件存储异常", e);
        return Result.error(500, e.getMessage());
    }

    @ExceptionHandler(FileUploadException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleFileUploadException(FileUploadException e) {
        log.warn("文件上传异常: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("请求参数校验失败");
        log.warn("参数校验失败: {}", msg);
        return Result.error(400, msg);
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("服务器内部异常", e);
        return Result.error(500, "服务器内部错误: " + e.getMessage());
    }
}
