package org.yeyr2.as12306.idempotent.enums;

// 幂等验证场景枚举
public enum IdempotentSceneEnum {
    // 基于RestAPI场景验证
    RESTAPI,
    // 基于MQ场景验证
    MQ;
}
