package com.opencast.screencast.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opencast.screencast.common.constant.SystemConstants;
import com.opencast.screencast.common.enums.ConvertStatus;
import com.opencast.screencast.common.enums.MaterialType;
import com.opencast.screencast.common.exception.BusinessException;
import com.opencast.screencast.common.result.ErrorCode;
import com.opencast.screencast.common.result.PageResult;
import com.opencast.screencast.dto.material.ConvertStatusResponse;
import com.opencast.screencast.dto.material.MaterialUploadResponse;
import com.opencast.screencast.dto.material.MaterialVO;
import com.opencast.screencast.entity.Material;
import com.opencast.screencast.mapper.MaterialMapper;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 素材服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialMapper materialMapper;
    private final MinioClient minioClient;
    private final ConvertService convertService;

    @Value("${minio.bucket:screen-cast}")
    private String bucketName;

    @Value("${minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    private static final List<String> IMAGE_TYPES = Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final List<String> VIDEO_TYPES = Arrays.asList("mp4", "avi", "mov", "mkv", "flv", "wmv");
    private static final List<String> PDF_TYPES = Arrays.asList("pdf");
    private static final List<String> PPT_TYPES = Arrays.asList("ppt", "pptx");
    private static final long MAX_FILE_SIZE = 500 * 1024 * 1024; // 500MB

    /**
     * 上传素材
     */
    @Transactional(rollbackFor = Exception.class)
    public MaterialUploadResponse upload(MultipartFile file, String name, Long categoryId, Long userId) {
        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEED);
        }

        // 获取文件信息
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String materialType = getMaterialType(extension);

        if (materialType == null) {
            throw new BusinessException(ErrorCode.FILE_FORMAT_NOT_SUPPORT);
        }

        // 生成存储路径
        String objectName = generateObjectName(extension);
        String pathPrefix = "materials/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        try {
            // 计算文件MD5
            String fileMd5 = org.springframework.util.DigestUtils.md5DigestAsHex(file.getInputStream());

            // 确保bucket存在
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
            }

            // 判断是否需要生成缩略图
            boolean needThumbnail = MaterialType.VIDEO.getCode().equals(materialType) 
                                 || MaterialType.PDF.getCode().equals(materialType)
                                 || MaterialType.IMAGE.getCode().equals(materialType);
            java.io.File tempFile = null;

            if (needThumbnail) {
                tempFile = java.io.File.createTempFile("upload_", "." + extension);
                file.transferTo(tempFile);
            }

            // 上传文件到MinIO
            if (needThumbnail) {
                try (InputStream inputStream = new java.io.FileInputStream(tempFile)) {
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(pathPrefix + "/" + objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
                }
            } else {
                try (InputStream inputStream = file.getInputStream()) {
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(pathPrefix + "/" + objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
                }
            }

            // 生成访问URL
            String fileUrl = minioEndpoint + "/" + bucketName + "/" + pathPrefix + "/" + objectName;

            // 创建素材记录
            Material material = new Material();
            material.setUserId(userId);
            material.setCategoryId(categoryId);
            material.setName(StrUtil.isNotBlank(name) ? name : originalFilename);
            material.setOriginalName(originalFilename);
            material.setType(materialType);
            material.setOriginalUrl(fileUrl);
            material.setFileSize(file.getSize());
            material.setFileMd5(fileMd5);
            material.setStatus(1);

            // 设置转码状态
            boolean needConvert = MaterialType.PPT.getCode().equals(materialType);
            if (needConvert) {
                material.setConvertStatus(ConvertStatus.CONVERTING.getCode());
                material.setConvertProgress(0);
            } else if (MaterialType.IMAGE.getCode().equals(materialType) || MaterialType.VIDEO.getCode().equals(materialType)) {
                material.setConvertStatus(ConvertStatus.NO_NEED.getCode());
            } else {
                material.setConvertStatus(ConvertStatus.NO_NEED.getCode());
            }

            materialMapper.insert(material);

            // 异步转换PPT为PDF
            if (needConvert) {
                convertService.convertPptToPdf(material.getId(), material.getOriginalUrl());
            }

            // 异步生成缩略图
            if (needThumbnail && tempFile != null) {
                convertService.generateThumbnailAsync(material.getId(), tempFile.getAbsolutePath(), materialType, originalFilename);
            }

            // 构建响应
            MaterialUploadResponse response = new MaterialUploadResponse();
            response.setId(material.getId());
            response.setName(material.getName());
            response.setType(material.getType());
            response.setOriginalUrl(material.getOriginalUrl());
            response.setFileSize(material.getFileSize());
            response.setConvertStatus(material.getConvertStatus());
            response.setNeedConvert(needConvert);

            return response;

        } catch (Exception e) {
            log.error("上传素材失败", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "上传素材失败: " + e.getMessage());
        }
    }

    /**
     * 获取素材列表
     */
    public PageResult<MaterialVO> getMaterialList(Long userId, String type, String keyword, Long page, Long size) {
        page = page == null ? SystemConstants.DEFAULT_PAGE : page;
        size = size == null ? SystemConstants.DEFAULT_SIZE : Math.min(size, SystemConstants.MAX_SIZE);

        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Material::getUserId, userId);
        wrapper.eq(Material::getStatus, 1);

        if (StrUtil.isNotBlank(type)) {
            wrapper.eq(Material::getType, type);
        }

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(Material::getName, keyword);
        }

        wrapper.orderByDesc(Material::getCreatedAt);

        Page<Material> pageResult = materialMapper.selectPage(new Page<>(page, size), wrapper);

        List<MaterialVO> list = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(pageResult.getTotal(), list, page, size);
    }

    /**
     * 获取素材详情
     */
    public MaterialVO getMaterialDetail(Long id, Long userId) {
        Material material = materialMapper.selectById(id);

        if (material == null || !userId.equals(material.getUserId()) || material.getStatus() != 1) {
            throw new BusinessException(ErrorCode.MATERIAL_NOT_FOUND);
        }

        return convertToVO(material);
    }

    /**
     * 查询转码状态
     */
    public ConvertStatusResponse getConvertStatus(Long id, Long userId) {
        Material material = materialMapper.selectById(id);

        if (material == null || !userId.equals(material.getUserId())) {
            throw new BusinessException(ErrorCode.MATERIAL_NOT_FOUND);
        }

        ConvertStatusResponse response = new ConvertStatusResponse();
        response.setId(material.getId());
        response.setConvertStatus(material.getConvertStatus());
        response.setConvertProgress(material.getConvertProgress());
        response.setConvertedUrl(material.getConvertedUrl());
        response.setThumbnailUrl(material.getThumbnailUrl());
        response.setPageCount(material.getPageCount());
        response.setFailReason(material.getConvertFailReason());

        return response;
    }

    /**
     * 删除素材
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMaterial(Long id, Long userId) {
        Material material = materialMapper.selectById(id);

        if (material == null || !userId.equals(material.getUserId())) {
            throw new BusinessException(ErrorCode.MATERIAL_NOT_FOUND);
        }

        // 软删除
        material.setStatus(0);
        materialMapper.updateById(material);

        // TODO: 异步删除MinIO中的文件
    }

    /**
     * 转换为VO
     */
    private MaterialVO convertToVO(Material material) {
        MaterialVO vo = new MaterialVO();
        vo.setId(material.getId());
        vo.setName(material.getName());
        vo.setOriginalName(material.getOriginalName());
        vo.setType(material.getType());
        vo.setOriginalUrl(material.getOriginalUrl());
        vo.setConvertedUrl(material.getConvertedUrl());
        vo.setThumbnailUrl(material.getThumbnailUrl());
        vo.setFileSize(material.getFileSize());
        vo.setDuration(material.getDuration());
        vo.setPageCount(material.getPageCount());
        vo.setFileMd5(material.getFileMd5());
        vo.setConvertStatus(material.getConvertStatus());
        vo.setCreatedAt(material.getCreatedAt());

        ConvertStatus status = ConvertStatus.of(material.getConvertStatus());
        vo.setConvertStatusDesc(status != null ? status.getDesc() : "未知");

        return vo;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (StrUtil.isBlank(filename)) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 获取素材类型
     */
    private String getMaterialType(String extension) {
        if (IMAGE_TYPES.contains(extension)) {
            return MaterialType.IMAGE.getCode();
        } else if (VIDEO_TYPES.contains(extension)) {
            return MaterialType.VIDEO.getCode();
        } else if (PDF_TYPES.contains(extension)) {
            return MaterialType.PDF.getCode();
        } else if (PPT_TYPES.contains(extension)) {
            return MaterialType.PPT.getCode();
        }
        return null;
    }

    /**
     * 生成对象存储名称
     */
    private String generateObjectName(String extension) {
        return UUID.randomUUID().toString().replace("-", "") + "." + extension;
    }
}
