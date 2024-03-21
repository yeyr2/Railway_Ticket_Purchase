package org.yeyr2.as12306.designpattern.chain;

import org.springframework.core.Ordered;

// 抽象业务责任链组件
public interface AbstractChainHandler<T> extends Ordered {

    // 执行责任链逻辑
    void handler(T requestParam);

    // 责任链组件标识
    String mark();
}
