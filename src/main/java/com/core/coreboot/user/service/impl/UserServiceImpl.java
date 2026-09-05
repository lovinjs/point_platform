package com.core.coreboot.user.service.impl;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.user.converter.UserConverter;
import com.core.coreboot.user.mapper.UserMapper;
import com.core.coreboot.user.entity.User;
import com.core.coreboot.user.service.UserService;
import com.core.coreboot.user.vo.LoginVO;
import com.core.coreboot.user.vo.UserVO;
import com.core.coreboot.utils.MD5Utils;
import com.core.coreboot.utils.TokenUtils;
import com.core.coreboot.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final UserConverter userConverter;

    @Override
    public void register(String userName, String password) throws CustomException {
        ValidationUtils.verifyUserParam(userName, password);
        long count = count(new LambdaQueryWrapper<User>().eq(User::getName, userName));
        if (count > 0) {
            throw new CustomException(ExceptionEnum.NAME_EXISTED);
        }
        User user = User.builder()
                .name(userName)
                .password(getMD5Password(password))
                .build();
        try {
            save(user);
        } catch (Exception e) {
            throw new CustomException(ExceptionEnum.INSERT_FAILED);
        }
    }

    @Override
    public LoginVO login(String userName, String password) throws CustomException {
        User user = this.getUserInfo(userName, password);
        UserVO userVO = userConverter.toUserVO(user);
        String token = TokenUtils.generateToken(user);
        return LoginVO.builder()
                .token(token)
                .tokenType("Bearer")
                .userInfo(userVO)
                .build();
    }

    @Override
    public User getUserInfo(String userName, String password) throws CustomException {
        ValidationUtils.verifyUserParam(userName, password);
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getName, userName)
                .eq(User::getPassword, getMD5Password(password))
        );
        if (user == null) {
            throw new CustomException(ExceptionEnum.WRONG_USERNAME_OR_PASSWORD);
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public void updateUserInfo(Integer id, String signature) throws CustomException {
        User user = User.builder()
                .id(id)
                .signature(signature)
                .build();
        try {
            boolean success = updateById(user);
            if (!success) {
                throw new CustomException(ExceptionEnum.UPDATE_FAILED);
            }
        } catch (Exception e) {
            throw new CustomException(ExceptionEnum.UPDATE_FAILED);
        }
    }

    @Override
    public boolean checkIsAdmin(User user) throws CustomException {
        return user.getRole().equals(2);
    }

    @Override
    public boolean checkIsAdmin(UserVO userVO) throws CustomException {
        return userVO.getRole().equals(2);
    }

    @Override
    public String getMD5Password(String password) throws RuntimeException {
        String md5Password;
        try {
            md5Password = MD5Utils.getMD5(password);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return md5Password;
    }
}
