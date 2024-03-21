package org.yeyr2.as12306.userService.common.constant;

// Redis key 定义常量类
public class RedisKeyConstant {
    //用户注册锁,key Prefix + 用户名
    public static final String LOCK_USER_REGISTER = "as12306-user-service:lock:user-register:";

    // 用户注销锁,key prefix + 用户名
    public static final String USER_DELETION = "as12306-user-service:user-deletion:";

    // 用户注册可复用用户名分片, key prefix + idx
    public static final String SUER_REGISTER_REUSE_SHARDING = "as12306-user-service:user-reuse:";

    //用户注册可复用用户名分片,key prefix + idx
    public  static final String USER_REGISTER_REUSE_SHARDING = "as12306-user-service:user_reuse:";

    // 用户乘车人列表,key prefix + 用户名
    public static final String USER_PASSENGER_LIST = "as12306-user-service:user-passenger-list:";
}
