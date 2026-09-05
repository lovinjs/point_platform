package com.core.coreboot.platform.auth.security;

import com.core.coreboot.platform.common.enums.RoleCode;
import com.core.coreboot.platform.common.enums.SysUserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.time.LocalDateTime;

public final class AdminUserPrincipal implements UserDetails {
    private final Long userId;
    private final String username;
    private final String passwordHash;
    private final String realName;
    private final SysUserStatus status;
    private final LocalDateTime lockedUntil;
    private final int tokenVersion;
    private final Set<RoleCode> roles;
    private final Set<Long> storeIds;
    private final Set<GrantedAuthority> authorities;

    public AdminUserPrincipal(
            Long userId,
            String username,
            String passwordHash,
            String realName,
            SysUserStatus status,
            LocalDateTime lockedUntil,
            int tokenVersion,
            Set<RoleCode> roles,
            Set<Long> storeIds
    ) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.realName = realName;
        this.status = status;
        this.lockedUntil = lockedUntil;
        this.tokenVersion = tokenVersion;
        this.roles = Set.copyOf(roles);
        this.storeIds = Set.copyOf(storeIds);
        this.authorities = this.roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public Long getUserId() {
        return userId;
    }

    public String getRealName() {
        return realName;
    }

    public Set<RoleCode> getRoles() {
        return roles;
    }

    public Set<Long> getStoreIds() {
        return storeIds;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != SysUserStatus.LOCKED
                && (lockedUntil == null || !lockedUntil.isAfter(LocalDateTime.now()));
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == SysUserStatus.ACTIVE;
    }
}
