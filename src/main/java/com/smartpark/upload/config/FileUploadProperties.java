package com.smartpark.upload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传安全配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "smartpark.upload")
public class FileUploadProperties {

    /**
     * 存储根目录
     */
    private String storagePath = "uploads";

    /**
     * 允许的扩展名白名单（小写带点）
     */
    private List<String> allowedExtensions = new ArrayList<>(List.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp",
            ".pdf", ".docx", ".xlsx", ".pptx"
    ));

    /**
     * 需要重编码的图片类型
     */
    private List<String> imageExtensions = new ArrayList<>(List.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp"
    ));

    /**
     * 单文件大小上限（MB）
     */
    private int maxFileSizeMb = 10;

    /**
     * 图片重编码内存上限（MB）
     */
    private int maxImageMemoryMb = 256;

    /**
     * 魔数映射表：魔数hex → 扩展名
     * key: 小写hex（如 "ffd8ff"），value: 扩展名（如 ".jpg"）
     */
    private Map<String, String> magicNumberMap = new HashMap<>(Map.of(
            "ffd8ff", ".jpg",
            "89504e47", ".png",
            "47494638", ".gif",
            "424d", ".bmp",
            "25504446", ".pdf",
            "504b0304", ".zip"
    ));

    /**
     * ZIP 内部特征文件映射：特征文件路径 → 扩展名
     */
    private Map<String, String> zipInnerFileMap = new HashMap<>(Map.of(
            "[Content_Types].xml", ".docx",
            "xl/workbook.xml", ".xlsx",
            "ppt/presentation.xml", ".pptx"
    ));

    /**
     * ZIP 最少条目数（防空 ZIP 攻击）
     */
    private int minZipEntries = 3;

    /**
     * 魔数检测时读取的头部字节数
     */
    private int magicNumberHeaderSize = 64;

    /**
     * 图片重编码 JPEG 压缩质量（0-1）
     */
    private float jpegQuality = 0.95f;
}
