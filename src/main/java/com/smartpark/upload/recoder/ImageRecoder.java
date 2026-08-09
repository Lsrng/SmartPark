package com.smartpark.upload.recoder;

import com.smartpark.upload.config.FileUploadProperties;
import com.smartpark.upload.exception.FileStorageException;
import com.smartpark.upload.util.MagicNumberUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * 图片重编码工具 - 防御 Polyglot 攻击（合法图片头 + 脚本体拼接）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageRecoder {

    private final FileUploadProperties properties;
    private final MagicNumberUtils magicNumberUtils;

    /**
     * 预估图片内存占用
     * @param inputStream 图片流
     * @param formatName ImageIO formatName
     * @return 预估内存（MB）
     */
    public long estimateMemory(InputStream inputStream, String formatName) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(formatName);
        if (!readers.hasNext()) {
            throw new FileStorageException("不支持的图片格式: " + formatName);
        }
        ImageReader reader = readers.next();
        reader.setInput(inputStream, true, false);
        try {
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            long estimatedMemory = (long) width * height * 4 / 1024 / 1024;
            log.debug("图片内存预估 - 宽: {}, 高: {}, 预估: {}MB", width, height, estimatedMemory);
            return estimatedMemory;
        } finally {
            reader.dispose();
        }
    }

    /**
     * 重编码图片
     * @param inputStream 原始流
     * @param extension 扩展名
     * @param outputStream 输出流
     */
    public void recode(InputStream inputStream, String extension, OutputStream outputStream) throws IOException {
        String formatName = magicNumberUtils.getImageFormatName(extension);
        if (formatName == null) {
            throw new FileStorageException("无法识别的图片格式: " + extension);
        }

        BufferedImage originalImage = ImageIO.read(inputStream);
        if (originalImage == null) {
            throw new FileStorageException("ImageIO 无法解析图片，可能为恶意伪装图片");
        }

        ImageIO.write(originalImage, formatName, outputStream);

        log.info("图片重编码完成 - 格式: {}, 原始: {}x{}", formatName, originalImage.getWidth(), originalImage.getHeight());
    }
}
