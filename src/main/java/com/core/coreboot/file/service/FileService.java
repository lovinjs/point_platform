package com.core.coreboot.file.service;

import com.core.coreboot.exception.CustomException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface FileService {

    String saveFile(MultipartFile file) throws CustomException;

    String saveImage(MultipartFile file, String path) throws CustomException;
}
