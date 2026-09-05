package com.core.coreboot;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;

import java.util.*;

public class MybatisPlusCodeGenerator {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/core_boot?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "123456";
    private static final String PROJECT_PATH = System.getProperty("user.dir");
    private static final String PACKAGE_BASE = "com.core.coreboot";

    public static void main(String[] args) {
        generateByTable("t_user");
    }

    /**
     * 根据表名生成 MP 模板代码
     * 如需生成多个表的模板代码，可多次调用此方法
     *
     * @param tableName 完整表名
     */
    public static void generateByTable(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            System.out.println("请指定表名！");
            return;
        }

        String moduleName = getModuleName(tableName);

        FastAutoGenerator.create(new DataSourceConfig.Builder(DB_URL, DB_USERNAME, DB_PASSWORD))
                .globalConfig(builder -> {
                    builder.outputDir(PROJECT_PATH + "/src/main/java") // 指定输出目录
                            .disableOpenDir() // 关闭打开输出目录
                            .dateType(DateType.TIME_PACK); // 实体类中的日期类型
                })
                .packageConfig(builder -> {
                    Map<OutputFile, String> pathInfo = new HashMap<>();
                    // XML文件路径：resources/mapper/{moduleName}
                    pathInfo.put(OutputFile.xml, PROJECT_PATH + "/src/main/resources/mapper/" + moduleName);
                    builder.parent(PACKAGE_BASE) // 基础包名
                            .moduleName(moduleName) // 模块名
                            .pathInfo(pathInfo); // XML文件输出路径
                })
                .injectionConfig(builder -> {
                    // 自定义配置
                    builder.beforeOutputFile((tableInfo, objectMap) -> {
                        System.out.println("Table: " + tableInfo.getEntityName() + " , File: " + tableInfo.getXmlName());
                    });
                })
                .strategyConfig(builder -> {
                    builder.addInclude(tableName) // 设置需要生成的表名
                            .addTablePrefix("t_", "sys_", "tb_") // 设置过滤表前缀
                            // Entity 策略配置
                            .entityBuilder()
                            .enableLombok() // 启用Lombok
                            .enableTableFieldAnnotation() // 生成字段注解
                            .naming(NamingStrategy.underline_to_camel) // 数据库表映射到实体的命名策略：下划线转驼峰
                            .columnNaming(NamingStrategy.underline_to_camel) // 数据库表字段映射到实体属性的命名策略
                            .idType(IdType.AUTO) // 主键策略
                            .formatFileName("%s") // 实体类文件名格式
                            .javaTemplate("/templates/entity.java")
                            // Service 策略配置
                            .serviceBuilder()
                            .formatServiceFileName("%sService") // Service文件名格式
                            .formatServiceImplFileName("%sServiceImpl") // ServiceImpl文件名格式
                            .serviceTemplate("/templates/service.java")
                            .serviceImplTemplate("/templates/serviceImpl.java")
                            // Mapper 策略配置
                            .mapperBuilder()
                            .formatMapperFileName("%sMapper") // Mapper文件名格式
                            .formatXmlFileName("%sMapper") // XML文件名格式
                            .mapperTemplate("/templates/mapper.java")
                            .mapperXmlTemplate("/templates/mapper.xml")
                            .controllerBuilder()
                            .template("/templates/controller.java");
                })
                // 使用Freemarker引擎模板
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }

    /**
     * 根据表名获取模块名
     *
     * @param tableName 表名
     * @return 模块名
     */
    private static String getModuleName(String tableName) {
        if (tableName == null) return "default";
        // 移除常见的表前缀，获取模块名
        if (tableName.startsWith("t_")) {
            return tableName.substring(2);
        } else if (tableName.startsWith("sys_")) {
            return tableName.substring(4);
        } else if (tableName.startsWith("tb_")) {
            return tableName.substring(3);
        } else {
            return tableName;
        }
    }
}
