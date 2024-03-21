package org.yeyr2.as12306.userService.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.yeyr2.as12306.convention.result.Result;
import org.yeyr2.as12306.userService.dto.req.UserDeletionReqDTO;
import org.yeyr2.as12306.userService.dto.req.UserRegisterReqDTO;
import org.yeyr2.as12306.userService.dto.req.UserUpdateReqDTO;
import org.yeyr2.as12306.userService.dto.resp.UserQueryActualRespDTO;
import org.yeyr2.as12306.userService.dto.resp.UserQueryRespDTO;
import org.yeyr2.as12306.userService.dto.resp.UserRegisterRespDTO;
import org.yeyr2.as12306.userService.service.UserLoginService;
import org.yeyr2.as12306.userService.service.UserService;
import org.yeyr2.as12306.web.Results;

/**
 * 用户控制层
 */
@RestController
@RequiredArgsConstructor
public class UserInfoController {

    private final UserLoginService userLoginService;
    private final UserService userService;

    /**
     * 根据用户名查询用户信息
     */
    @GetMapping("/api/user-service/query")
    public Result<UserQueryRespDTO> queryUserByUsername(@RequestParam("username") @NotEmpty String username) {
        return Results.success((UserQueryRespDTO) userService.queryUserByUsername(username,false));
    }

    /**
     * 根据用户名查询用户无脱敏信息
     */
    @GetMapping("/api/user-service/actual/query")
    public Result<UserQueryActualRespDTO> queryActualUserByUsername(@RequestParam("username") @NotEmpty String username) {
        return Results.success((UserQueryActualRespDTO) userService.queryUserByUsername(username,true));
    }

    /**
     * 检查用户名是否已存在
     */
    @GetMapping("/api/user-service/has-username")
    public Result<Boolean> hasUsername(@RequestParam("username") @NotEmpty String username) {
        return Results.success(userLoginService.hasUsername(username));
    }

    /**
     * 注册用户
     */
    @PostMapping("/api/user-service/register")
    public Result<UserRegisterRespDTO> register(@RequestBody @Valid UserRegisterReqDTO requestParam) {
        return Results.success(userLoginService.register(requestParam));
    }

    /**
     * 修改用户
     */
    @PostMapping("/api/user-service/update")
    public Result<Void> update(@RequestBody @Valid UserUpdateReqDTO requestParam) {
        userService.update(requestParam);
        return Results.success();
    }

    /**
     * 注销用户
     */
    @PostMapping("/api/user-service/deletion")
    public Result<Void> deletion(@RequestBody @Valid UserDeletionReqDTO requestParam) {
        userLoginService.deletion(requestParam);
        return Results.success();
    }
}
