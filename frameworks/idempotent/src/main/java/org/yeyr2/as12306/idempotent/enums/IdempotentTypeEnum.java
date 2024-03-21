package org.yeyr2.as12306.idempotent.enums;

// 幂等验证类型枚举
public enum IdempotentTypeEnum {
    // 基于Token方式验证
    TOKEN,
    // 基于方法参数方式验证
    PARAM,
    // 基于SpEL表达式方式验证
    SPEL;
}
