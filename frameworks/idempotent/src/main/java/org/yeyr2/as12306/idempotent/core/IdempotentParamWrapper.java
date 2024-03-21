package org.yeyr2.as12306.idempotent.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.aspectj.lang.ProceedingJoinPoint;
import org.yeyr2.as12306.idempotent.annotation.Idempotent;
import org.yeyr2.as12306.idempotent.enums.IdempotentTypeEnum;

// 幂等参数包装
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class IdempotentParamWrapper {
    // 幂等注解
    private Idempotent idempotent;

    // AOP处理连接点
    private ProceedingJoinPoint joinPoint;

    /**
     * 锁标识,{@link IdempotentTypeEnum#PARAM}
     */
    private String lockKey;
}
