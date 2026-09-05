package com.core.coreboot.user.service;

import com.core.coreboot.exception.CustomException;
import com.core.coreboot.user.entity.User;
import com.core.coreboot.user.vo.LoginVO;
import com.core.coreboot.user.vo.UserVO;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.IService;

@Service
public interface UserService extends IService<User> {
    void register(String userName, String password) throws CustomException;

    LoginVO login(String userName, String password) throws CustomException;

    User getUserInfo(String userName, String password) throws CustomException;

    void updateUserInfo(Integer id, String signature) throws CustomException;

    boolean checkIsAdmin(User user) throws CustomException;

    boolean checkIsAdmin(UserVO userVO) throws CustomException;

    String getMD5Password(String password) throws RuntimeException;
}
