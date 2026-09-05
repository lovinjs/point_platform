package com.core.coreboot.user.entity;

import lombok.Data;
import lombok.Builder;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private String password;

    private String signature;

    private Integer role;

    private Integer isDeleted;

    private Date createTime;

    private Date updateTime;
}