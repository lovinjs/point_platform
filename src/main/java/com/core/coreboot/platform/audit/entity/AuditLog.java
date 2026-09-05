package com.core.coreboot.platform.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.core.coreboot.platform.common.enums.AuditActorType;
import com.core.coreboot.platform.common.enums.RoleCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private AuditActorType actorType;
    private Long actorId;
    private RoleCode operatorRole;
    private Long storeId;
    private String action;
    private String resourceType;
    private String resourceNo;
    private String requestId;
    private String beforeSnapshot;
    private String afterSnapshot;
    private String remark;
    private String clientIp;
    private LocalDateTime createTime;
}
