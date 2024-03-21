package org.yeyr2.as12306.distributedid.toolkit;

import org.yeyr2.as12306.distributedid.core.snowflake.Snowflake;
import org.yeyr2.as12306.distributedid.core.snowflake.SnowflakeIdInfo;
import org.yeyr2.as12306.distributedid.handler.IdGeneratorManager;

// 分布式雪花ID生成器
public class SnowflakeIdUtil {
    // 雪花算法对象
    private static Snowflake SNOWFLAKE;

    // 初始雪花算法
    public static void initSnowflake(Snowflake snowflake){
        SnowflakeIdUtil.SNOWFLAKE = snowflake;
    }

    // 获取雪花算法实例
    public static Snowflake getInstance(){
        return SNOWFLAKE;
    }

    // 获取雪花算法下一个ID
    public  static long nextId(){
        return SNOWFLAKE.nextId();
    }

    // 获取雪花算法下一个字符串类型ID
    public static String nextIDStr(){
        return Long.toString(nextId());
    }

    /**
     * 根据 {@param serviceId} 生成字符串类型雪花算法 ID
     */
    public static String nextIdStrByService(String serviceId) {
        return IdGeneratorManager.getDefaultServiceIdGenerator().nextIDStr(Long.parseLong(serviceId));
    }

    /**
     * 根据 {@param serviceId} 生成字符串类型雪花算法 ID
     */
    public static String nextIdStrByService(String resource, long serviceId) {
        return IdGeneratorManager.getIdGenerator(resource).nextIDStr(serviceId);
    }

    /**
     * 根据 {@param serviceId} 生成字符串类型雪花算法 ID
     */
    public static String nextIdStrByService(String resource, String serviceId) {
        return IdGeneratorManager.getIdGenerator(resource).nextIDStr(serviceId);
    }

    /**
     * 解析雪花算法生成的 ID 为对象
     */
    public static SnowflakeIdInfo parseSnowflakeServiceId(String snowflakeId) {
        return IdGeneratorManager.getDefaultServiceIdGenerator().parseSnowflakeId(Long.parseLong(snowflakeId));
    }

    /**
     * 解析雪花算法生成的 ID 为对象
     */
    public static SnowflakeIdInfo parseSnowflakeServiceId(String resource, String snowflakeId) {
        return IdGeneratorManager.getIdGenerator(resource).parseSnowflakeId(Long.parseLong(snowflakeId));
    }
}
