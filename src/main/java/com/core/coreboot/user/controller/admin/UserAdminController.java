package com.core.coreboot.user.controller.admin;

import com.core.coreboot.common.ApiRestResponse;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.user.service.UserService;
import com.core.coreboot.user.vo.LoginVO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "用户模块 - 管理员")
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/user")
public class UserAdminController {

    private final UserService userService;

    @Operation(summary = "登录", description = "管理员登录")
    @PostMapping("/login")
    @ResponseBody
    public ApiRestResponse<Object> adminLogin(
            @Parameter(description = "用户名") @RequestParam("userName") String userName,
            @Parameter(description = "用户密码") @RequestParam("password") String password
    ) {
        LoginVO loginVO = userService.login(userName, password);
        if (userService.checkIsAdmin(loginVO.getUserInfo())) {
            return ApiRestResponse.success(loginVO);
        } else {
            return ApiRestResponse.error(ExceptionEnum.NEED_ADMIN);
        }
    }
}
