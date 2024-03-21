package org.yeyr2.as12306.bizs.user.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDTO {
    //用户id
    private String userId;
    //用户名
    private String username;
    //用户真实姓名
    private String realName;
    //用户token
    private String token;
}
