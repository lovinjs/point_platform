package com.core.coreboot.file.controller;

import com.core.coreboot.common.ApiRestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.core.coreboot.file.service.FileService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

@Tag(name = "文件模块", description = "文件模块")
@RestController
@RequiredArgsConstructor
@RequestMapping("/upload")
public class FileController {
    private final FileService fileService;

    @Operation(summary = "文件上传", description = "文件上传")
    @PostMapping("/file")
    public ApiRestResponse<Object> uploadFile(@Parameter(description = "上传文件") @Valid @RequestParam("file") MultipartFile file) {
        String savedPath = fileService.saveFile(file);
        return ApiRestResponse.success(savedPath);
    }

    @Operation(summary = "产品图片上传", description = "产品图片上传")
    @PostMapping("/image/product")
    public ApiRestResponse<Object> uploadProductImage(@Parameter(description = "上传图片") @Valid @RequestParam("file") MultipartFile file) {
        String savedPath = fileService.saveImage(file, "product");
        return ApiRestResponse.success(savedPath);
    }

    @Operation(summary = "轮播图片上传", description = "轮播图片上传")
    @PostMapping("/image/banner")
    public ApiRestResponse<Object> uploadBannerImage(@Parameter(description = "上传图片") @Valid @RequestParam("file") MultipartFile file) {
        String savedPath = fileService.saveImage(file, "banner");
        return ApiRestResponse.success(savedPath);
    }
}
