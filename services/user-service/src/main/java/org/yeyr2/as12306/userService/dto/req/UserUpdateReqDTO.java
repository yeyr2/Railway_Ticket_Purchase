package org.yeyr2.as12306.userService.dto.req;


import lombok.Data;

/**
 * 用户修改请求参数
 */
@Data
public class UserUpdateReqDTO extends UserReqDTO{

    /**
     * 用户id
     */
    private String id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String mail;

    /**
     * 旅客类型
     */
    private Integer userType;

    /**
     * 邮编
     */
    private String postCode;

    /**
     * 地址
     */
    private String address;
}
