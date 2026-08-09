package com.smartpark.upload.controller;

import com.smartpark.common.result.Result;
import com.smartpark.upload.config.FileUploadProperties;
import com.smartpark.upload.pojo.dto.UploadResult;
import com.smartpark.upload.service.FileUploadService;
import com.smartpark.upload.service.FileUploadService.DownloadResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Tag(name = "文件上传", description = "安全文件上传接口（双重校验：后缀白名单 + 魔数检测）")
public class FileUploadController {

    private final FileUploadService fileUploadService;
    private final FileUploadProperties properties;

    @PostMapping("/upload")
    @Operation(summary = "单文件上传", description = "通过后缀白名单 + 魔数双重校验机制进行安全文件上传")
    public Result<UploadResult> upload(
            @Parameter(description = "上传文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "业务分组（如 enterprise-register、property-repair）")
            @RequestParam(value = "bucket", defaultValue = "default") String bucket) {

        log.info("文件上传请求 - bucket: {}, 文件名: {}, 大小: {}bytes",
                bucket, file.getOriginalFilename(), file.getSize());

        UploadResult result = fileUploadService.upload(file, bucket);

        log.info("文件上传成功 - storagePath: {}, 存储名: {}",
                result.getStoragePath(), result.getStoredName());

        return Result.success("上传成功", result);
    }

    @PostMapping("/batch-upload")
    @Operation(summary = "多文件批量上传")
    public Result<java.util.List<UploadResult>> batchUpload(
            @Parameter(description = "上传文件数组") @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "业务分组") @RequestParam(value = "bucket", defaultValue = "default") String bucket) {

        java.util.List<UploadResult> results = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            results.add(fileUploadService.upload(file, bucket));
        }
        return Result.success("批量上传成功", results);
    }

    @GetMapping("/download")
    @Operation(summary = "文件下载", description = "通过 storagePath 下载文件，强制 attachment + nosniff 安全头")
    public ResponseEntity<Resource> download(
            @Parameter(description = "存储路径（bucket/yyyy/MM/dd/uuid.ext）")
            @RequestParam("storagePath") String storagePath) {

        log.info("文件下载请求 - storagePath: {}", storagePath);

        DownloadResult result = fileUploadService.download(storagePath);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.getStoredName() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType(result.getMimeType()))
                .contentLength(result.getFileData().length)
                .body(new ByteArrayResource(result.getFileData()));
    }

    @DeleteMapping
    @Operation(summary = "文件删除", description = "通过 storagePath 删除文件")
    public Result<Void> delete(
            @Parameter(description = "存储路径") @RequestParam("storagePath") String storagePath) {

        log.info("文件删除请求 - storagePath: {}", storagePath);

        fileUploadService.delete(storagePath);
        return Result.success("删除成功");
    }
}
