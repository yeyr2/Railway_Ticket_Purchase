package org.yeyr2.as12306.idempotent.core.spel;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.yeyr2.as12306.cache.DistributedCache;
import org.yeyr2.as12306.idempotent.annotation.Idempotent;
import org.yeyr2.as12306.idempotent.core.*;
import org.yeyr2.as12306.idempotent.enums.IdempotentMQConsumeStatusEnum;
import org.yeyr2.as12306.idempotent.toolkit.LogUtil;
import org.yeyr2.as12306.idempotent.toolkit.SpELUtil;

import java.util.concurrent.TimeUnit;

// 基于SpEL方法检验请求幂等性,适用于MQ场景
@RequiredArgsConstructor
public final class IdempotentSpELByMQExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentSpELService {

    private final DistributedCache distributedCache;
    private final static int TIMEOUT = 600;
    private final static String WRAPPER = "wrapper:spEL:MQ";

    @SneakyThrows
    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        Idempotent idempotent = IdempotentAspect.getIdempotentAnnotation(joinPoint);
        String key = (String) SpELUtil.parseKey(
                idempotent.key(),
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
        Boolean setIfAbsent = ((StringRedisTemplate) distributedCache.getInstance())
                .opsForValue()
                .setIfAbsent(uniqueKey, IdempotentMQConsumeStatusEnum.CONSUMING.getCode(),TIMEOUT, TimeUnit.SECONDS);
        if (setIfAbsent != null && !setIfAbsent){
            String consumeStatus = distributedCache.get(uniqueKey,String.class);
            boolean error = IdempotentMQConsumeStatusEnum.isError(consumeStatus);
            LogUtil.getLog(wrapper.getJoinPoint()).warn("[{}] MQ repeated consumption, {}",
                    uniqueKey,error ? "Wait for the client to delay consumption" : "Status is completed");
            throw new RepeatConsumptionException(error);
        }
        IdempotentContext.put(WRAPPER,wrapper);
    }

    @Override
    public void exceptionProcessing() {
        IdempotentParamWrapper wrapper = (IdempotentParamWrapper) IdempotentContext.getKey(WRAPPER);
        if(wrapper != null){
            Idempotent idempotent = wrapper.getIdempotent();
            String uniqueKey = idempotent.uniqueKeyPrefix() + wrapper.getLockKey();
            try{
                distributedCache.delete(uniqueKey);
            }catch (Throwable ex){
                LogUtil.getLog(wrapper.getJoinPoint()).error("[{}] Failed to del MQ anti-heavy token.",uniqueKey);
            }
        }
    }

    @Override
    public void postProcessing() {
        IdempotentParamWrapper wrapper = (IdempotentParamWrapper) IdempotentContext.getKey(WRAPPER);
        if(wrapper != null){
            Idempotent idempotent = wrapper.getIdempotent();
            String uniqueKey = idempotent.uniqueKeyPrefix() + wrapper.getLockKey();
            try{
                distributedCache.put(uniqueKey,IdempotentMQConsumeStatusEnum.CONSUMED.getCode(), idempotent.keyTimeout(), TimeUnit.SECONDS);
            }catch (Throwable ex){
                LogUtil.getLog(wrapper.getJoinPoint()).error("[{}] Failed to del MQ anti-heavy token.",uniqueKey);
            }
        }
    }
}
