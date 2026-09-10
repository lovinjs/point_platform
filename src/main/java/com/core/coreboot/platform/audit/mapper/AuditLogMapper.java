package com.core.coreboot.platform.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.core.coreboot.platform.audit.entity.AuditLog;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AuditLogMapper extends BaseMapper<AuditLog> {
    @Select("""
            SELECT DISTINCT action
            FROM t_audit_log
            ORDER BY action
            """)
    List<String> selectDistinctActions();
}
