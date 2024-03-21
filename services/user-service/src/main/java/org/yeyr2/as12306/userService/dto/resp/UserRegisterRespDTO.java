package org.yeyr2.as12306.userService.dto.resp;

import lombok.Data;

/**
 * 用户注册返回参数
 */
@Data
public class UserRegisterRespDTO extends UserRespDTO{

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;
}
