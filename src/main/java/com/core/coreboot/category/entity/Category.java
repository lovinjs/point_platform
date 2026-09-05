package com.core.coreboot.category.entity;

import lombok.Data;
import lombok.Builder;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Category {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private Integer rank;

    @TableField("`order`")
    private Integer order;

    private Integer parentId;

    private Integer isDeleted;

    private Date createTime;

    private Date updateTime;
}