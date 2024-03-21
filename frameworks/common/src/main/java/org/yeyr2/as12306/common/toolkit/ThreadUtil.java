package org.yeyr2.as12306.common.toolkit;

import lombok.SneakyThrows;

// 线程池工具类
public class ThreadUtil {

    // 睡眠当前线程指定时间
    @SneakyThrows(value = InterruptedException.class)
    public static void sleep(long millis){
        Thread.sleep(millis);
    }
}
