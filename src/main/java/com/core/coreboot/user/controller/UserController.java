package com.core.coreboot.user.controller;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.filter.UserRoleFilter;
import com.core.coreboot.user.entity.User;
import com.core.coreboot.user.vo.LoginVO;
import com.core.coreboot.user.vo.UserVO;
import com.core.coreboot.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;


@Tag(name = "用户模块 - 用户")
@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @Operation(summary = "注册", description = "用户注册")
    @PostMapping("/register")
    @ResponseBody
    public ApiRestResponse<Object> register(
            @Parameter(description = "用户名") @RequestParam("userName") String userName,
            @Parameter(description = "用户密码") @RequestParam("password") String password
    ) throws CustomException {
        userService.register(userName, password);
        return ApiRestResponse.success();
    }

    @Operation(summary = "登录", description = "用户登录")
    @PostMapping("/login")
    @ResponseBody
    public ApiRestResponse<Object> login(
            @Parameter(description = "用户名") @RequestParam("userName") String userName,
            @Parameter(description = "用户密码") @RequestParam("password") String password
    ) {
        LoginVO loginVO = userService.login(userName, password);
        return ApiRestResponse.success(loginVO);
    }

    @Operation(summary = "更新用户", description = "更新用户个人信息")
    @PostMapping("/update")
    @ResponseBody
    public ApiRestResponse<Object> updateUserInfo(@Parameter(description = "个性签名") @RequestParam String signature) {
        User currentUser = getCurrentUser();
        userService.updateUserInfo(currentUser.getId(), signature);
        return ApiRestResponse.success();
    }

    @Operation(summary = "获取用户信息", description = "获取用户个人信息")
    @GetMapping("/info")
    @ResponseBody
    public ApiRestResponse<Object> getUserInfo() {
        User currentUser = getCurrentUser();
        UserVO userVO = UserVO.builder()
                .id(currentUser.getId())
                .name(currentUser.getName())
                .signature(currentUser.getSignature())
                .role(currentUser.getRole())
                .build();
        return ApiRestResponse.success(userVO);
    }

    public User getCurrentUser() throws CustomException {
        User currentUser = UserRoleFilter.getCurrentUser();
        if (currentUser == null) {
            throw new CustomException(ExceptionEnum.NEED_LOGGED);
        }
        return currentUser;
    }
}
