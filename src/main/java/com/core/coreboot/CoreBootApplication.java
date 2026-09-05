package com.core.coreboot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@MapperScan("com.core.coreboot.**.mapper")
@EnableCaching
public class CoreBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreBootApplication.class, args);
    }

}
