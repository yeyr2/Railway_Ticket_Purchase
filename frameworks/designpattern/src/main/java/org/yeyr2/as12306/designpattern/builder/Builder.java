package org.yeyr2.as12306.designpattern.builder;

import java.io.Serializable;

// Builder模式抽象接口
public interface Builder<T> extends Serializable {
    // 构建方法
    T build();
}
