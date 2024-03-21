package org.yeyr2.as12306.userService.dto.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *  用户登录请求参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginReqDTO extends UserReqDTO{

    /**
     * 用户名
     */
     private String usernameOrMailOrPhone;

    /**
     * 密码
     */
    private String password;
}
