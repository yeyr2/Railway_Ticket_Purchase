package org.yeyr2.as12306.distributedid.core.serviceid;

import org.yeyr2.as12306.distributedid.core.snowflake.SnowflakeIdInfo;

// 业务ID生成器
public interface ServiceIdGenerator {
    // 根据@param serviceId生成雪花算法Id
    default long nextId(long serviceId){
        return 0L;
    }

    // 根据@param serviceId生成雪花算法Id
    default long nextId(String serviceId){
        return 0L;
    }

    //根据@param serviceId生成字符串类型雪花算法Id
    default String nextIDStr(long serviceId){
        return null;
    }

    //根据@param serviceId生成字符串类型雪花算法Id
    default String nextIDStr(String serviceId){
        return null;
    }

    // 解析雪花算法
    SnowflakeIdInfo parseSnowflakeId(long snowflakeId);
}
