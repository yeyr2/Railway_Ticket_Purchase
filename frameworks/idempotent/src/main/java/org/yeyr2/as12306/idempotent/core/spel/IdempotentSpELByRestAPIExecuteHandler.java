package org.yeyr2.as12306.idempotent.core.spel;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.yeyr2.as12306.convention.exception.ClientException;
import org.yeyr2.as12306.idempotent.annotation.Idempotent;
import org.yeyr2.as12306.idempotent.core.AbstractIdempotentExecuteHandler;
import org.yeyr2.as12306.idempotent.core.IdempotentAspect;
import org.yeyr2.as12306.idempotent.core.IdempotentContext;
import org.yeyr2.as12306.idempotent.core.IdempotentParamWrapper;
import org.yeyr2.as12306.idempotent.toolkit.SpELUtil;

//基于 SpEL 方法验证请求幂等性，适用于 RestAPI 场景
@RequiredArgsConstructor
public final class IdempotentSpELByRestAPIExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentSpELService {

    private final RedissonClient redissonClient;
    private final static String LOCK = "lock:spEL:restAPI";

    @SneakyThrows
    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        Idempotent idempotent = IdempotentAspect.getIdempotentAnnotation(joinPoint);
        String key = (String) SpELUtil.parseKey(idempotent.key(),
                ((MethodSignature)joinPoint.getSignature()).getMethod(),
                joinPoint.getArgs());
        return IdempotentParamWrapper
                .builder()
                .lockKey(key)
                .joinPoint(joinPoint)
                .build();
    }

    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        String uniqueKey = wrapper.getIdempotent().uniqueKeyPrefix() + wrapper.getLockKey();
        RLock lock = redissonClient.getLock(uniqueKey);
        if(!lock.tryLock()){
            throw new ClientException(wrapper.getIdempotent().message());
        }
        IdempotentContext.put(LOCK,lock);
    }

    @Override
    public void postProcessing() {
        RLock lock = null;
        try {
            lock = (RLock) IdempotentContext.getKey(LOCK);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }


    @Override
    public void exceptionProcessing() {
        postProcessing();
    }
}
