package com.core.coreboot.user.mapper;

import org.springframework.stereotype.Repository;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.core.coreboot.user.entity.User;

@Repository
public interface UserMapper extends BaseMapper<User> {
}