package com.core.coreboot.file.service.impl;

import com.core.coreboot.common.Constant;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.file.service.FileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {
    @Override
    public String saveFile(MultipartFile file) throws CustomException {
        // 获取文件名
        String originalFilename = file.getOriginalFilename();
        // 获取绝对路径，创建文件夹
        String projectRoot = System.getProperty("user.dir");
        Path uploadPath = Paths.get(projectRoot, Constant.UPLOAD_FILE_PATH).toAbsolutePath().normalize();
        File fileDirectory = new File(uploadPath.toString());
        // 创建目标文件
        File targetFile = new File(uploadPath.toString() + "/" + originalFilename);
        if (!fileDirectory.exists()) {
            if (!fileDirectory.mkdirs()) {
                throw new CustomException(ExceptionEnum.MKDIR_FAILED);
            }
        }
        // 将上传的文件传输给目标文件
        try {
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new CustomException(ExceptionEnum.FILE_UPLOAD_FAILED);
        }
        return "/files/" + originalFilename;
    }

    @Override
    public String saveImage(MultipartFile file, String path) throws CustomException {
        // 获取文件名 以及 文件后缀名
        String originalFilename = file.getOriginalFilename();
        String suffixName = null;
        if (originalFilename != null) {
            suffixName = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        }
        // 只允许上传图片格式
        if (!Constant.ALLOWED_IMAGE_EXTENSIONS.contains(suffixName)) {
            throw new CustomException(ExceptionEnum.ONLY_IMAGE_ALLOWED);
        }
        // 生成 uuid 随机文件名字
        UUID uuid = UUID.randomUUID();
        String newFileName = uuid.toString() + "." + suffixName;
        // 获取绝对路径，创建文件夹
        String projectRoot = System.getProperty("user.dir");
        Path uploadPath = Paths.get(projectRoot, Constant.UPLOAD_IMAGE_PATH, path).toAbsolutePath().normalize();
        File fileDirectory = new File(uploadPath.toString());
        // 创建目标文件
        File targetFile = new File(uploadPath.toString() + "/" + newFileName);
        if (!fileDirectory.exists()) {
            if (!fileDirectory.mkdirs()) {
                throw new CustomException(ExceptionEnum.MKDIR_FAILED);
            }
        }
        // 将上传的文件传输给目标文件
        try {
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new CustomException(ExceptionEnum.IMAGE_UPLOAD_FAILED);
        }
        return "/images/" + path + "/" + newFileName;
    }
}
