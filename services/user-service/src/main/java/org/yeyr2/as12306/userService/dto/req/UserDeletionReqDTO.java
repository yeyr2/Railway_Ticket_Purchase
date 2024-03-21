package org.yeyr2.as12306.userService.dto.req;

import lombok.Data;

/**
 * 用户注销请求参数
 */
@Data
public class UserDeletionReqDTO extends UserReqDTO{

    /**
     * 用户名
     */
    private String username;
}
