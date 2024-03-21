package org.yeyr2.as12306.idempotent.core;

import org.aspectj.lang.ProceedingJoinPoint;
import org.yeyr2.as12306.idempotent.annotation.Idempotent;

// 觅得执行执行器
public interface IdempotentExecuteHandler {
    /**
     * 幂等处理逻辑
     *
     * @param wrapper 幂等参数包装器
     */
    void handler(IdempotentParamWrapper wrapper);

    /**
     * 执行幂等处理逻辑
     *
     * @param joinPoint  AOP 方法处理
     * @param idempotent 幂等注解
     */
    void execute(ProceedingJoinPoint joinPoint, Idempotent idempotent);

    /**
     * 异常处理流程
     */
    default void exceptionProcessing(){

    }

    /**
     * 后置处理
     */
    default void postProcessing(){

    }
}
