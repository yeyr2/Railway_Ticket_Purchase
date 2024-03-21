package org.yeyr2.as12306.userService.service;

import org.yeyr2.as12306.userService.dto.req.UserDeletionReqDTO;
import org.yeyr2.as12306.userService.dto.req.UserLoginReqDTO;
import org.yeyr2.as12306.userService.dto.req.UserRegisterReqDTO;
import org.yeyr2.as12306.userService.dto.resp.UserLoginRespDTO;
import org.yeyr2.as12306.userService.dto.resp.UserRegisterRespDTO;

/**
 * 用户登录接口
 */
public interface UserLoginService {

    /**
     * 用户登录接口
     *
     * @param req 用户登录参数
     * @return 用户登录返回结果
     */
    UserLoginRespDTO login(UserLoginReqDTO req);


    /**
     * 通过token验证用户是否登录
     *
     * @param accessToken
     * @return
     */
    UserLoginRespDTO checkLogin(String accessToken);

    /**
     * 用户退出登录
     *
     * @param accessToken 用户登录token凭证
     */
    void logout(String accessToken);

    /**
     * 用户名是否存在
     *
     * @param username 用户名
     * @return 用户名是否存在
     */
    Boolean hasUsername(String username);

    /**
     * 用户注册
     *
     * @param registerParam 用户注册参数
     * @return 用户注册结果
     */
    UserRegisterRespDTO register(UserRegisterReqDTO registerParam);

    /**
     * 注销用户
     *
     * @param requestParam 注销用户参数
     */
    void deletion(UserDeletionReqDTO requestParam);
}
