package com.smartpark.upload.service.Impl;

import com.smartpark.upload.config.FileUploadProperties;
import com.smartpark.upload.exception.*;
import com.smartpark.upload.mapper.FileMetadataMapper;
import com.smartpark.upload.pojo.dto.UploadResult;
import com.smartpark.upload.pojo.entity.FileMetadata;
import com.smartpark.upload.recoder.ImageRecoder;
import com.smartpark.upload.service.FileUploadService;
import com.smartpark.upload.util.MagicNumberUtils;
import com.smartpark.upload.util.MagicNumberUtils.DetectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final FileUploadProperties properties;
    private final MagicNumberUtils magicNumberUtils;
    private final ImageRecoder imageRecoder;
    private final FileMetadataMapper fileMetadataMapper;

    @Override
    @Transactional
    public UploadResult upload(MultipartFile file, String bucket) {
        // Step 1: 空值校验
        validateFileNotNull(file);

        // Step 2: 文件大小校验
        long maxSize = (long) properties.getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new FileSizeExceededException(
                    String.format("文件大小 %d 超过最大限制 %dMB", file.getSize(), properties.getMaxFileSizeMb()));
        }

        // Step 2.5: 文件名安全预处理
        String originalFilename = file.getOriginalFilename();
        String cleanFilename = sanitizeFilename(originalFilename);
        String userExtension = extractExtension(cleanFilename);

        // Step 3: 后缀白名单校验
        if (!userExtension.isEmpty()) {
            if (!magicNumberUtils.isAllowedExtension(userExtension)) {
                throw new ExtensionNotAllowedException(
                        String.format("文件扩展名 %s 不在允许列表中", userExtension));
            }
        }

        // Step 4: 魔数校验（核心防御）
        DetectionResult detectionResult;
        try (InputStream is = file.getInputStream()) {
            detectionResult = magicNumberUtils.detectFileType(is);
        } catch (IOException e) {
            throw new FileStorageException("读取文件流失败", e);
        }

        if (detectionResult.getDetectedExtension() == null) {
            throw new FileTypeUnknownException("无法识别的文件类型");
        }
        if (!magicNumberUtils.isAllowedExtension(detectionResult.getDetectedExtension())) {
            throw new MagicMismatchException(
                    String.format("文件真实类型 %s 不在允许列表中", detectionResult.getDetectedExtension()));
        }

        String detectedExtension = detectionResult.getDetectedExtension();

        // Step 4.5: 图片重编码（Polyglot 防御）
        InputStream storageStream;
        long finalFileSize;

        if (magicNumberUtils.isImageExtension(detectedExtension)) {
            RecodeResult recodeResult = recodeImage(file, detectionResult, detectedExtension);
            storageStream = recodeResult.stream();
            finalFileSize = recodeResult.size();
            if (finalFileSize > maxSize) {
                throw new FileSizeExceededException(
                        String.format("图片重编码后大小 %d 超过最大限制 %dMB", finalFileSize, properties.getMaxFileSizeMb()));
            }
        } else {
            try {
                storageStream = detectionResult.reassembleStream(file);
                finalFileSize = file.getSize();
            } catch (IOException e) {
                throw new FileStorageException("重建文件流失败", e);
            }
        }

        // Step 5: UUID 重命名
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String storedName = uuid + detectedExtension;

        // Step 6: 目录组织
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = bucket + "/" + datePath + "/" + storedName;
        Path directory = Paths.get(properties.getStoragePath(), bucket,
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy")),
                LocalDate.now().format(DateTimeFormatter.ofPattern("MM")),
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd")));

        // Step 7: 存储执行
        Path targetPath = Paths.get(properties.getStoragePath(), relativePath);
        try {
            Files.createDirectories(directory);
            Files.copy(storageStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("文件存储失败", e);
        }

        // Step 7.5: 元信息持久化
        String mimeType = magicNumberUtils.getMimeType(detectedExtension);
        FileMetadata metadata = FileMetadata.builder()
                .storagePath(relativePath)
                .bucket(bucket)
                .storedName(storedName)
                .originalName(originalFilename != null ? originalFilename : storedName)
                .realExtension(detectedExtension)
                .mimeType(mimeType)
                .fileSize(finalFileSize)
                .build();

        try {
            fileMetadataMapper.insert(metadata);
        } catch (Exception e) {
            log.error("文件元信息写入数据库失败，回滚删除已存储文件: {}", targetPath, e);
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException delEx) {
                log.error("回滚删除文件失败: {}", targetPath, delEx);
            }
            throw new FileStorageException("文件元信息持久化失败", e);
        }

        // Step 8: 返回结果
        return UploadResult.builder()
                .storagePath(relativePath)
                .originalName(originalFilename != null ? originalFilename : storedName)
                .storedName(storedName)
                .fileSize(finalFileSize)
                .fileExtension(detectedExtension)
                .downloadUrl("/api/file/download?storagePath=" + relativePath)
                .build();
    }

    @Override
    public DownloadResult download(String storagePath) {
        validatePathSafety(storagePath);

        FileMetadata metadata = fileMetadataMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<FileMetadata>query()
                        .eq("storage_path", storagePath));

        if (metadata == null) {
            throw new FileStorageException("文件元信息不存在: " + storagePath);
        }

        Path filePath = Paths.get(properties.getStoragePath(), storagePath);
        if (!Files.exists(filePath)) {
            throw new FileStorageException("文件不存在: " + storagePath);
        }

        try {
            byte[] data = Files.readAllBytes(filePath);
            return new DownloadResult(data, metadata.getMimeType(), metadata.getStoredName());
        } catch (IOException e) {
            throw new FileStorageException("读取文件失败", e);
        }
    }

    @Override
    public void delete(String storagePath) {
        validatePathSafety(storagePath);

        FileMetadata metadata = fileMetadataMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<FileMetadata>query()
                        .eq("storage_path", storagePath));

        if (metadata == null) {
            log.warn("文件元信息不存在，跳过删除: {}", storagePath);
            return;
        }

        Path filePath = Paths.get(properties.getStoragePath(), storagePath);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("磁盘文件删除失败: {}", filePath, e);
        }

        try {
            fileMetadataMapper.deleteById(metadata.getId());
        } catch (Exception e) {
            log.warn("元信息删除失败，可定期清理: storagePath={}", storagePath, e);
        }
    }

    // ==================== 私有方法 ====================

    private void validateFileNotNull(MultipartFile file) {
        if (file == null) {
            throw new FileEmptyException("上传文件不能为空");
        }
        if (file.isEmpty()) {
            throw new FileEmptyException("上传文件为空");
        }
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new InvalidFilenameException("文件名为空");
        }

        if (originalFilename.contains("\0") || originalFilename.contains("%00")) {
            throw new InvalidFilenameException("文件名包含非法空字节字符");
        }

        String clean = originalFilename.trim();
        clean = clean.replaceAll("[.\\s]+$", "");

        if (clean.isEmpty()) {
            throw new InvalidFilenameException("文件名清理后为空");
        }

        return clean;
    }

    private String extractExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex).toLowerCase();
    }

    private void validatePathSafety(String storagePath) {
        if (storagePath == null || storagePath.isEmpty()) {
            throw new InvalidFilenameException("存储路径不能为空");
        }
        if (storagePath.contains("..") || storagePath.contains(":") || storagePath.contains("\\")) {
            throw new InvalidFilenameException("存储路径包含非法字符");
        }
    }

    private RecodeResult recodeImage(MultipartFile file, DetectionResult detectionResult, String extension) {
        String formatName = magicNumberUtils.getImageFormatName(extension);
        try {
            InputStream streamForEstimation = detectionResult.reassembleStream(file);
            long estimatedMemory = imageRecoder.estimateMemory(streamForEstimation, formatName);
            streamForEstimation.close();

            if (estimatedMemory > (long) properties.getMaxImageMemoryMb()) {
                throw new FileStorageException(
                        String.format("图片内存预估 %dMB 超过限制 %dMB", estimatedMemory, properties.getMaxImageMemoryMb()));
            }

            InputStream streamForRecode = detectionResult.reassembleStream(file);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            imageRecoder.recode(streamForRecode, extension, baos);
            streamForRecode.close();

            byte[] recodedBytes = baos.toByteArray();
            return new RecodeResult(new ByteArrayInputStream(recodedBytes), recodedBytes.length);
        } catch (IOException e) {
            throw new FileStorageException("图片重编码失败，可能为恶意伪装图片", e);
        }
    }

    private record RecodeResult(InputStream stream, long size) {
    }
}
