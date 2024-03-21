package org.yeyr2.as12306.common.threadpool.proxy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Proxy;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.atomic.AtomicLong;

// 拒绝策略代理工具类
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RejectedProxyUtil {
    // 创建拒绝策略代理类
    public static RejectedExecutionHandler createProxy(RejectedExecutionHandler rejectedExecutionHandler,
                                                       AtomicLong rejectedNum){
        // 动态代理模式: 增强线程池拒绝策略，比如：拒绝任务报警或加入延迟队列重复放入等逻辑
        return  (RejectedExecutionHandler) Proxy.newProxyInstance(
                    rejectedExecutionHandler.getClass().getClassLoader(),
                    new Class[]{RejectedExecutionHandler.class},
                    new RejectedProxyInvocationHandler(rejectedExecutionHandler,rejectedNum)
        );
    }
}
