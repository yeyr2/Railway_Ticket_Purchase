package org.yeyr2.as12306.designpattern.chain;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;
import org.yeyr2.as12306.base.ApplicationContextHolder;

import java.util.*;
import java.util.stream.Collectors;

// 抽象责任链上下文
public class AbstractChainContext<T> implements CommandLineRunner {

    private final Map<String, List<AbstractChainHandler>> abstractChainHandlerContainer = new HashMap<>();

    // 责任链组件执行
    public void handler(String mark,T requestParam){
        List<AbstractChainHandler> abstractChainHandlers = abstractChainHandlerContainer.get(mark);
        if(CollectionUtils.isEmpty(abstractChainHandlers)){
            throw new RuntimeException(String.format("[%s] Chain of Responsibility ID is undefined.",mark));
        }
        abstractChainHandlers.forEach(each -> each.handler(requestParam));
    }

    @Override
    public void run(String... args) throws Exception {
        Map<String, AbstractChainHandler> chainHandlerMap = ApplicationContextHolder.getBeansOfType(AbstractChainHandler.class);
        chainHandlerMap.forEach((beanName, bean) -> {
            List<AbstractChainHandler> abstractChainHandlerContainers = abstractChainHandlerContainer.get(bean.mark());
            if(CollectionUtils.isEmpty(abstractChainHandlerContainers)){
                abstractChainHandlerContainers = new ArrayList<>();
            }
            abstractChainHandlerContainers.add(bean);
            List<AbstractChainHandler> actualAbstractChainHandlers = abstractChainHandlerContainers.stream()
                    .sorted(Comparator.comparing(Ordered::getOrder))
                    .collect(Collectors.toList());
            abstractChainHandlerContainer.put(bean.mark(),actualAbstractChainHandlers);
        });
    }
}
