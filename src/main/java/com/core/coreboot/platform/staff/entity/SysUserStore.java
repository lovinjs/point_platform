package com.core.coreboot.platform.staff.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_sys_user_store")
public class SysUserStore {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long storeId;
    private LocalDateTime createTime;
}
