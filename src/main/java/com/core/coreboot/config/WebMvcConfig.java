package com.core.coreboot.config;

import com.core.coreboot.common.Constant;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 配置地址映射
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String projectRoot = System.getProperty("user.dir");
        Path uploadFilePath = Paths.get(projectRoot, Constant.UPLOAD_FILE_PATH).toAbsolutePath().normalize();
        Path uploadImagePath = Paths.get(projectRoot, Constant.UPLOAD_IMAGE_PATH).toAbsolutePath().normalize();
        Path qrcodeGeneratePath = Paths.get(projectRoot, Constant.QRCODE_GENERATE_PATH).toAbsolutePath().normalize();
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadFilePath.toString() + "/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadImagePath.toString() + "/");
        registry.addResourceHandler("/qrcode/**")
                .addResourceLocations("file:" + qrcodeGeneratePath.toString() + "/");
    }
}
