package com.opencast.screencast.service;

import cn.hutool.core.io.IoUtil;
import cn.hutool.http.HttpUtil;
import com.opencast.screencast.common.enums.ConvertStatus;
import com.opencast.screencast.entity.Material;
import com.opencast.screencast.mapper.MaterialMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.opencast.screencast.common.enums.MaterialType;

/**
 * 文件转换服务（PPT转PDF等）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConvertService {

    private final MaterialMapper materialMapper;
    private final MinioClient minioClient;

    @Value("${gotenberg.url:http://localhost:3000}")
    private String gotenbergUrl;

    @Value("${minio.bucket:screencast}")
    private String bucketName;

    @Value("${minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    /**
     * 异步转换PPT为PDF
     */
    @Async
    public void convertPptToPdf(Long materialId, String originalUrl) {
        log.info("Starting PPT to PDF conversion for material: {}", materialId);

        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            log.error("Material not found: {}", materialId);
            return;
        }

        try {
            // 更新状态为转换中
            material.setConvertStatus(ConvertStatus.CONVERTING.getCode());
            material.setConvertProgress(10);
            materialMapper.updateById(material);

            // 下载原始文件
            byte[] fileContent = downloadFile(originalUrl);

            // 更新进度
            material.setConvertProgress(30);
            materialMapper.updateById(material);

            // 调用Gotenberg进行转换
            byte[] pdfContent = convertWithGotenberg(fileContent, material.getOriginalName());

            // 更新进度
            material.setConvertProgress(70);
            materialMapper.updateById(material);

            // 上传转换后的PDF到MinIO
            String pdfObjectName = generatePdfName(material.getOriginalName());
            String pdfPath = uploadToMinio(pdfContent, pdfObjectName, "application/pdf");

            // 更新素材记录
            material.setConvertedUrl(pdfPath);
            material.setConvertStatus(ConvertStatus.SUCCESS.getCode());
            material.setConvertProgress(100);
            materialMapper.updateById(material);

            log.info("PPT to PDF conversion completed for material: {}", materialId);

            // 生成预览图（基于转换后的PDF）
            try {
                File tempPdf = File.createTempFile("converted_", ".pdf");
                cn.hutool.core.io.FileUtil.writeBytes(pdfContent, tempPdf);
                // 直接调用缩略图生成逻辑（此处由于在同一类中，@Async不会生效，即同步执行，这在异步转换任务中是合理的）
                generateThumbnailAsync(materialId, tempPdf.getAbsolutePath(), MaterialType.PDF.getCode(), material.getOriginalName());
            } catch (Exception e) {
                log.error("Failed to trigger thumbnail generation for converted PPT: {}", materialId, e);
            }

        } catch (Exception e) {
            log.error("PPT to PDF conversion failed for material: {}", materialId, e);
            material.setConvertStatus(ConvertStatus.FAILED.getCode());
            material.setConvertFailReason(e.getMessage());
            materialMapper.updateById(material);
        }
    }

    /**
     * 异步生成缩略图（支持视频和PDF）
     */
    @Async
    public void generateThumbnailAsync(Long materialId, String tempFilePath, String materialType, String originalName) {
        log.info("Starting thumbnail generation for material: {}", materialId);

        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            log.error("Material not found: {}", materialId);
            return;
        }

        File tempFile = new File(tempFilePath);
        try {
            byte[] thumbnailBytes = null;
            if (MaterialType.VIDEO.getCode().equals(materialType)) {
                thumbnailBytes = generateVideoThumbnail(tempFile);
            } else if (MaterialType.PDF.getCode().equals(materialType)) {
                try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.Loader.loadPDF(tempFile)) {
                    material.setPageCount(document.getNumberOfPages());
                    thumbnailBytes = generatePdfThumbnail(document);
                }
            } else if (MaterialType.IMAGE.getCode().equals(materialType)) {
                thumbnailBytes = generateImageThumbnail(tempFile);
            }

            if (thumbnailBytes != null && thumbnailBytes.length > 0) {
                String thumbName = "thumb_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";
                String thumbPath = uploadToMinio(thumbnailBytes, thumbName, "image/jpeg");
                material.setThumbnailUrl(thumbPath);
                materialMapper.updateById(material);
                log.info("Thumbnail generated and uploaded successfully for material: {}", materialId);
            } else {
                log.warn("Generated thumbnail is null or empty for material: {}", materialId);
            }
        } catch (Exception e) {
            log.error("Failed to generate thumbnail for material: {}", materialId, e);
        } finally {
            if (tempFile.exists()) {
                if (!tempFile.delete()) {
                    log.warn("Failed to delete temporary file: {}", tempFilePath);
                }
            }
        }
    }

    /**
     * 获取图片缩略图（等比例缩放压缩）
     */
    private byte[] generateImageThumbnail(File file) throws Exception {
        java.awt.image.BufferedImage srcImage = cn.hutool.core.img.ImgUtil.read(file);
        if (srcImage == null) {
            return null;
        }

        int width = srcImage.getWidth();
        int height = srcImage.getHeight();
        
        int maxDim = 800; // 缩略图最大边长
        int targetWidth = width;
        int targetHeight = height;
        
        if (width > maxDim || height > maxDim) {
            if (width > height) {
                targetWidth = maxDim;
                targetHeight = (int) (height * ((float) maxDim / width));
            } else {
                targetHeight = maxDim;
                targetWidth = (int) (width * ((float) maxDim / height));
            }
        }
        
        java.awt.Image scaledImage = cn.hutool.core.img.ImgUtil.scale(srcImage, targetWidth, targetHeight);
        try (java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream()) {
            cn.hutool.core.img.ImgUtil.write(scaledImage, cn.hutool.core.img.ImgUtil.IMAGE_TYPE_JPEG, os);
            return os.toByteArray();
        }
    }

    /**
     * 获取视频缩略图
     */
    private byte[] generateVideoThumbnail(File file) throws Exception {
        try (org.bytedeco.javacv.FFmpegFrameGrabber grabber = new org.bytedeco.javacv.FFmpegFrameGrabber(file)) {
            grabber.start();
            // 默认截取第1秒，如果视频不足1秒则截取一半
            long duration = grabber.getLengthInTime();
            long seekTime = duration > 2000000 ? 1000000 : duration / 2;
            grabber.setVideoTimestamp(seekTime);

            org.bytedeco.javacv.Frame frame = null;
            while (frame == null || frame.image == null) {
                frame = grabber.grabImage();
                if (frame == null) break;
            }

            if (frame != null && frame.image != null) {
                try (org.bytedeco.javacv.Java2DFrameConverter converter = new org.bytedeco.javacv.Java2DFrameConverter()) {
                    java.awt.image.BufferedImage bi = converter.convert(frame);
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    javax.imageio.ImageIO.write(bi, "jpg", baos);
                    return baos.toByteArray();
                }
            }
            grabber.stop();
        }
        return null;
    }

    /**
     * 获取PDF缩略图
     */
    private byte[] generatePdfThumbnail(org.apache.pdfbox.pdmodel.PDDocument document) throws Exception {
        org.apache.pdfbox.rendering.PDFRenderer pdfRenderer = new org.apache.pdfbox.rendering.PDFRenderer(document);
        // 渲染第一页 (DPI: 100)
        java.awt.image.BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 100, org.apache.pdfbox.rendering.ImageType.RGB);
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(bim, "jpg", baos);
            return baos.toByteArray();
        }
    }

    /**
     * 下载文件
     */
    private byte[] downloadFile(String url) throws Exception {
        if (!url.startsWith("http")) {
            url = minioEndpoint + "/" + bucketName + "/" + url;
        }
        return HttpUtil.downloadBytes(url);
    }

    /**
     * 使用Gotenberg转换文件
     */
    private byte[] convertWithGotenberg(byte[] fileContent, String originalName) throws Exception {
        String convertUrl = gotenbergUrl + "/forms/libreoffice/convert";

        // 构建multipart请求
        Map<String, Object> formData = new HashMap<>();
        formData.put("file", fileContent);
        formData.put("filename", originalName);

        // 使用Hutool发送请求
        byte[] result = HttpUtil.createPost(convertUrl)
                .form(formData)
                .execute()
                .bodyBytes();

        if (result == null || result.length == 0) {
            throw new RuntimeException("Gotenberg conversion returned empty result");
        }

        return result;
    }

    /**
     * 上传到MinIO
     */
    private String uploadToMinio(byte[] content, String objectName, String contentType) throws Exception {
        String pathPrefix = "converted/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(pathPrefix + "/" + objectName)
                    .stream(inputStream, content.length, -1)
                    .contentType(contentType)
                    .build());
        }

        return minioEndpoint + "/" + bucketName + "/" + pathPrefix + "/" + objectName;
    }

    /**
     * 生成PDF文件名
     */
    private String generatePdfName(String originalName) {
        String baseName = originalName;
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = originalName.substring(0, dotIndex);
        }
        return baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
    }
}
