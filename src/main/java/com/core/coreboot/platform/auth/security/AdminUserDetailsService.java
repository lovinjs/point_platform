package com.core.coreboot.platform.auth.security;

import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.staff.entity.SysUser;
import com.core.coreboot.platform.staff.mapper.StaffAuthorityMapper;
import com.core.coreboot.platform.staff.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {
    private final SysUserMapper sysUserMapper;
    private final StaffAuthorityMapper staffAuthorityMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw notFound();
        }
        SysUser user = sysUserMapper.selectByUsername(username.trim());
        return buildPrincipal(user);
    }

    public AdminUserPrincipal loadByUserId(Long userId) throws UsernameNotFoundException {
        if (userId == null || userId <= 0) {
            throw notFound();
        }
        return buildPrincipal(sysUserMapper.selectById(userId));
    }

    private AdminUserPrincipal buildPrincipal(SysUser user) {
        if (user == null) {
            throw notFound();
        }

        Set<RoleCode> roles = parseRoles(staffAuthorityMapper.selectRoleCodes(user.getId()));
        if (roles.isEmpty()) {
            throw new UsernameNotFoundException("后台账号权限配置异常");
        }
        List<Long> assignedStoreIds = staffAuthorityMapper.selectStoreIds(user.getId());
        Set<Long> storeIds = assignedStoreIds == null ? Set.of() : new HashSet<>(assignedStoreIds);

        return new AdminUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRealName(),
                user.getStatus(),
                user.getLockedUntil(),
                user.getTokenVersion() == null ? 0 : user.getTokenVersion(),
                roles,
                storeIds
        );
    }

    private Set<RoleCode> parseRoles(List<String> roleCodes) {
        Set<RoleCode> roles = EnumSet.noneOf(RoleCode.class);
        if (roleCodes == null) {
            return roles;
        }
        try {
            roleCodes.forEach(code -> roles.add(RoleCode.valueOf(code)));
            return roles;
        } catch (IllegalArgumentException ex) {
            throw new UsernameNotFoundException("后台账号权限配置异常", ex);
        }
    }

    private UsernameNotFoundException notFound() {
        return new UsernameNotFoundException("后台账号不存在");
    }
}
