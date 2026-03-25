package com.opencast.screencast.controller;

import com.opencast.screencast.common.result.PageResult;
import com.opencast.screencast.common.result.Result;
import com.opencast.screencast.dto.material.ConvertStatusResponse;
import com.opencast.screencast.dto.material.MaterialUploadResponse;
import com.opencast.screencast.dto.material.MaterialVO;
import com.opencast.screencast.service.MaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 素材控制器
 */
@Tag(name = "素材管理", description = "素材上传、列表、删除、转码状态")
@RestController
@RequestMapping("/api/v1/material")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @Operation(summary = "上传素材", description = "上传图片、视频、PDF、PPT等素材")
    @PostMapping("/upload")
    public Result<MaterialUploadResponse> upload(
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "素材名称") @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "分类ID") @RequestParam(value = "category", required = false) Long category,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return Result.success(materialService.upload(file, name, category, userId));
    }

    @Operation(summary = "素材列表", description = "获取素材列表，支持按类型和关键词筛选")
    @GetMapping("/list")
    public Result<PageResult<MaterialVO>> getMaterialList(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "类型筛选: image/video/pdf/ppt") @RequestParam(required = false) String type,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(required = false) Long page,
            @Parameter(description = "每页大小") @RequestParam(required = false) Long size) {
        return Result.success(materialService.getMaterialList(userId, type, keyword, page, size));
    }

    @Operation(summary = "素材详情", description = "获取素材详细信息")
    @GetMapping("/{id}")
    public Result<MaterialVO> getMaterialDetail(
            @Parameter(description = "素材ID") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return Result.success(materialService.getMaterialDetail(id, userId));
    }

    @Operation(summary = "删除素材", description = "删除指定素材")
    @DeleteMapping("/{id}")
    public Result<Void> deleteMaterial(
            @Parameter(description = "素材ID") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        materialService.deleteMaterial(id, userId);
        return Result.success();
    }

    @Operation(summary = "转码状态", description = "查询PPT转PDF等转码状态")
    @GetMapping("/{id}/convert-status")
    public Result<ConvertStatusResponse> getConvertStatus(
            @Parameter(description = "素材ID") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return Result.success(materialService.getConvertStatus(id, userId));
    }
}
