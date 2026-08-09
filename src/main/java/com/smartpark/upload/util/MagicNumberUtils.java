package com.smartpark.upload.util;

import com.smartpark.upload.config.FileUploadProperties;
import com.smartpark.upload.exception.FileTypeUnknownException;
import com.smartpark.upload.exception.MagicMismatchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 魔数校验工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MagicNumberUtils {

    private final FileUploadProperties properties;

    /**
     * 魔数检测结果
     */
    public static class DetectionResult {
        private final String detectedExtension;
        private final byte[] headerBytes;
        private final boolean isZipType;

        public DetectionResult(String detectedExtension, byte[] headerBytes, boolean isZipType) {
            this.detectedExtension = detectedExtension;
            this.headerBytes = headerBytes;
            this.isZipType = isZipType;
        }

        public String getDetectedExtension() {
            return detectedExtension;
        }

        public byte[] getHeaderBytes() {
            return headerBytes;
        }

        public boolean isZipType() {
            return isZipType;
        }

        /**
         * 基于原始 MultipartFile + 缓存的 header 字节，重建完整流
         */
        public InputStream reassembleStream(MultipartFile file) throws IOException {
            InputStream freshStream = file.getInputStream();
            return new SequenceInputStream(new ByteArrayInputStream(headerBytes), freshStream);
        }

        /**
         * 基于剩余流重建完整流（非 ZIP 场景使用）
         */
        public InputStream reassembleStream(InputStream remainingStream) {
            return new SequenceInputStream(new ByteArrayInputStream(headerBytes), remainingStream);
        }
    }

    /**
     * 检测文件真实类型
     */
    public DetectionResult detectFileType(InputStream inputStream) throws IOException {
        int headerSize = properties.getMagicNumberHeaderSize();
        byte[] header = new byte[headerSize];
        int readLen = inputStream.read(header);

        if (readLen <= 0) {
            throw new FileTypeUnknownException("无法读取文件内容，文件可能为空");
        }

        byte[] actualHeader = readLen < headerSize ? Arrays.copyOf(header, readLen) : header;
        String hexHeader = bytesToHex(actualHeader);

        Map<String, String> sortedMagicMap = properties.getMagicNumberMap().entrySet().stream()
                .sorted(Map.Entry.<String, String>comparingByKey().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));

        for (Map.Entry<String, String> entry : sortedMagicMap.entrySet()) {
            String magic = entry.getKey();
            String extension = entry.getValue();
            if (hexHeader.startsWith(magic)) {
                if (".jpg".equals(extension)) {
                    if (!isValidJpeg(actualHeader)) {
                        throw new MagicMismatchException("JPEG 魔数二次校验失败：第4字节非合法APP标记");
                    }
                }
                if (".zip".equals(extension)) {
                    return detectZipInternalStructure(inputStream, actualHeader);
                }
                return new DetectionResult(extension, actualHeader, false);
            }
        }

        throw new FileTypeUnknownException("无法识别的文件类型");
    }

    /**
     * JPEG 二次校验：验证第4字节为合法APP标记（0xE0-0xEF）
     */
    private boolean isValidJpeg(byte[] header) {
        if (header.length < 4) {
            return false;
        }
        int fourthByte = header[3] & 0xFF;
        return fourthByte >= 0xE0 && fourthByte <= 0xEF;
    }

    /**
     * ZIP 内部结构检测
     */
    private DetectionResult detectZipInternalStructure(InputStream inputStream, byte[] headerBytes) throws IOException {
        Set<String> matchedFeatures = new HashSet<>();
        int entryCount = 0;
        int maxEntries = 1000;

        List<Map.Entry<String, String>> sortedInnerMap = properties.getZipInnerFileMap().entrySet().stream()
                .sorted(Map.Entry.<String, String>comparingByKey().reversed())
                .toList();

        InputStream reassembledStream = new SequenceInputStream(new ByteArrayInputStream(headerBytes), inputStream);

        try (ZipInputStream zis = new ZipInputStream(reassembledStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null && entryCount < maxEntries) {
                entryCount++;
                String entryName = entry.getName();
                for (Map.Entry<String, String> innerEntry : sortedInnerMap) {
                    String featurePath = innerEntry.getKey();
                    if (entryName.equals(featurePath) || entryName.endsWith("/" + featurePath)) {
                        matchedFeatures.add(featurePath);
                    }
                }
                zis.closeEntry();
            }
        }

        if (entryCount < properties.getMinZipEntries()) {
            throw new MagicMismatchException("ZIP 条目数过少，可能为空 ZIP 攻击");
        }

        String detectedExtension = null;
        if (matchedFeatures.contains("xl/workbook.xml")) {
            detectedExtension = ".xlsx";
        } else if (matchedFeatures.contains("ppt/presentation.xml")) {
            detectedExtension = ".pptx";
        } else if (matchedFeatures.contains("[Content_Types].xml")) {
            detectedExtension = ".docx";
        }

        if (detectedExtension == null) {
            throw new MagicMismatchException("ZIP 内部结构不匹配任何允许的文档格式");
        }

        return new DetectionResult(detectedExtension, headerBytes, true);
    }

    /**
     * 判断扩展名是否在白名单中
     */
    public boolean isAllowedExtension(String extension) {
        if (extension == null) {
            return false;
        }
        return properties.getAllowedExtensions().contains(extension.toLowerCase());
    }

    /**
     * 判断是否为图片类型（需要重编码）
     */
    public boolean isImageExtension(String extension) {
        if (extension == null) {
            return false;
        }
        return properties.getImageExtensions().contains(extension.toLowerCase());
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    /**
     * 根据扩展名获取 MIME 类型
     */
    public String getMimeType(String extension) {
        return switch (extension.toLowerCase()) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".bmp" -> "image/bmp";
            case ".pdf" -> "application/pdf";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default -> "application/octet-stream";
        };
    }

    /**
     * 根据扩展名获取 ImageIO formatName
     */
    public String getImageFormatName(String extension) {
        return switch (extension.toLowerCase()) {
            case ".jpg", ".jpeg" -> "jpg";
            case ".png" -> "png";
            case ".gif" -> "gif";
            case ".bmp" -> "bmp";
            default -> null;
        };
    }
}
