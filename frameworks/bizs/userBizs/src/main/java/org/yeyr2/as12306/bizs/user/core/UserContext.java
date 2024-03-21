package org.yeyr2.as12306.bizs.user.core;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Optional;

// 线程用户上下文管理
public class UserContext {

    private static final ThreadLocal<UserInfoDTO> USER_THEAD_LOCAL = new TransmittableThreadLocal<>();

    public static void setUser(UserInfoDTO user){
        USER_THEAD_LOCAL.set(user);
    }

    // 获取上下文中的用户id
    public static String getUserId(){
        UserInfoDTO userInfoDTO = USER_THEAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getUserId).orElse(null);
    }

    // 获取上下文中的用户名
    public static String getUsername(){
        UserInfoDTO userInfoDTO = USER_THEAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getUsername).orElse(null);
    }

    public static String getRealName(){
        UserInfoDTO userInfoDTO = USER_THEAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getRealName).orElse(null);
    }

    public static String getToken(){
        UserInfoDTO userInfoDTO = USER_THEAD_LOCAL.get();
        return Optional.ofNullable(userInfoDTO).map(UserInfoDTO::getToken).orElse(null);
    }

    public static void removeUserContent(){
        USER_THEAD_LOCAL.remove();
    }
}
