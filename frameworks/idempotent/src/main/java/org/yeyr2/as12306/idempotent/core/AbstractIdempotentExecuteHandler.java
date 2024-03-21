package org.yeyr2.as12306.idempotent.core;

import org.aspectj.lang.ProceedingJoinPoint;
import org.yeyr2.as12306.idempotent.annotation.Idempotent;

// 抽象幂等执行器
public abstract class AbstractIdempotentExecuteHandler implements IdempotentExecuteHandler{
    /**
     * 构建幂等验证过程中所需要的参数包装器
     *
     * @param joinPoint AOP 方法处理
     * @return 幂等参数包装器
     */
    protected abstract IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint);

    /**
     * 执行幂等处理逻辑
     *
     * @param joinPoint  AOP 方法处理
     * @param idempotent 幂等注解
     */
    public void execute(ProceedingJoinPoint joinPoint, Idempotent idempotent){
        // 模式方法模式:构建幂等包装器
        IdempotentParamWrapper idempotentParamWrapper = buildWrapper(joinPoint).setIdempotent(idempotent);
        handler(idempotentParamWrapper);
    }
}
