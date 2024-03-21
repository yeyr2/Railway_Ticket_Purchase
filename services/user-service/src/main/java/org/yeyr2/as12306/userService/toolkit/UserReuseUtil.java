package org.yeyr2.as12306.userService.toolkit;

import org.yeyr2.as12306.userService.common.constant.As12306Constant;

// 用户名可复用工具类
public class UserReuseUtil {
    //  计算分片位置
    public static int hashShardingUdx(String username){
        return Math.abs(username.hashCode()) % As12306Constant.USER_REGISTER_REUSE_SHARDING_COUNT;
    }
}
