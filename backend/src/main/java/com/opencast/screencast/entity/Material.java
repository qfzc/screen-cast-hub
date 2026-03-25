package com.opencast.screencast.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.opencast.screencast.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 素材实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("material")
public class Material extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 素材名称
     */
    private String name;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 类型: image/video/pdf/ppt
     */
    private String type;

    /**
     * 原始文件URL
     */
    private String originalUrl;

    /**
     * 转换后URL
     */
    private String convertedUrl;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件MD5
     */
    private String fileMd5;

    /**
     * 时长(秒)，视频素材专用
     */
    private Integer duration;

    /**
     * 宽度，图片/视频专用
     */
    private Integer width;

    /**
     * 高度，图片/视频专用
     */
    private Integer height;

    /**
     * 页数，PDF/PPT专用
     */
    private Integer pageCount;

    /**
     * 转码状态: 0无需 1转码中 2成功 3失败
     */
    private Integer convertStatus;

    /**
     * 转码进度(%)
     */
    private Integer convertProgress;

    /**
     * 转码失败原因
     */
    private String convertFailReason;

    /**
     * 状态: 1正常 0删除
     */
    private Integer status;
}
