package com.core.coreboot.user.converter;

import com.core.coreboot.user.entity.User;
import com.core.coreboot.user.vo.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserConverter {
    UserVO toUserVO(User user);
}

