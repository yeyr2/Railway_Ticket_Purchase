package org.yeyr2.as12306.idempotent.annotation;

import org.yeyr2.as12306.idempotent.enums.IdempotentSceneEnum;
import org.yeyr2.as12306.idempotent.enums.IdempotentTypeEnum;

import java.lang.annotation.*;

// 幂等注解
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    /**
     * 幂等Key，只有在 {@link Idempotent#type()} 为 {@link IdempotentTypeEnum#SPEL} 时生效
     */
    String key() default "";

    /**
     * 触发幂等失败逻辑时,返回的错误提示信息
     */
    String message() default "操作太快,请重试";

    /**
     * 验证幂等类型,支持多种幂等方式RestAPI,建议使用{@link IdempotentTypeEnum#TOKEN} 或 {@link IdempotentTypeEnum#PARAM}
     * 其他类型幂等验证,使用{@link IdempotentTypeEnum#SPEL}
     */
    IdempotentTypeEnum type() default IdempotentTypeEnum.PARAM;

    /**
     * 验证幂等场景,支持多种{@link IdempotentSceneEnum}
     */
    IdempotentSceneEnum scene() default IdempotentSceneEnum.RESTAPI;

    /**
     * 设置防重令牌key前缀,MQ幂等去重可选设置{@link IdempotentSceneEnum#MQ}and {@link IdempotentTypeEnum#SPEL}时生效
     */
    String uniqueKeyPrefix() default  "";

    /**
     * 设置防重令牌key过期时间,单位秒,默认1h,MQ幂等去重可选设置{@link IdempotentSceneEnum#MQ} and {@link IdempotentTypeEnum#SPEL}时生效
     * @return
     */
    long keyTimeout() default 3600L;
}
