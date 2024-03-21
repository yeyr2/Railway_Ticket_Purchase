package org.yeyr2.as12306.designpattern.strategy;

import org.springframework.context.ApplicationListener;
import org.springframework.util.StringUtils;
import org.yeyr2.as12306.base.ApplicationContextHolder;
import org.yeyr2.as12306.base.init.ApplicationInitializingEvent;
import org.yeyr2.as12306.convention.exception.ServiceException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

// 策略选择器
public class AbstractStrategyChoose implements ApplicationListener<ApplicationInitializingEvent> {

    // 执行策略集合
    private final Map<String, AbstractExecuteStrategy> abstractExecuteStrategyMap = new HashMap<>();

    /**
     * 根据 mark 查询具体策略并执行
     *
     * @param mark          策略标识
     * @param requestParam  执行策略入参
     * @param predicateFlag 匹配范解析标识
     * @param <T>     执行策略入参范型
     */
    public <T> void chooseAndExecute(String mark, T requestParam, Boolean predicateFlag) {
        AbstractExecuteStrategy executeStrategy = choose(mark, predicateFlag);
        executeStrategy.execute(requestParam);
    }

    // 根据mark查询具体策略
    public AbstractExecuteStrategy choose(String mark,Boolean predicateFlag){
        if(predicateFlag != null && predicateFlag){
            return abstractExecuteStrategyMap.values().stream()
                    .filter(each -> StringUtils.hasText(each.patternMatchMark()))
                    .filter(each -> Pattern.compile(each.patternMatchMark()).matcher(mark).matches())
                    .findFirst()
                    .orElseThrow(() -> new ServiceException("策略未定义"));
        }
        return Optional.ofNullable(abstractExecuteStrategyMap.get(mark))
                .orElseThrow(() -> new ServiceException(String.format("[%s] 策略未定义",mark)));
    }

    // 根据mark查询具体策略并执行
    public <T> void chooseAndExecute(String mark,T requestParam){
        AbstractExecuteStrategy executeStrategy = choose(mark,null);
        executeStrategy.execute(requestParam);
    }

    /**
     * 根据 mark 查询具体策略并执行，带返回结果
     * @param mark 策略标识
     * @param requestParam 执行策略入参
     * @return 执行策略出参范型
     * @param <REQUEST> 执行策略入参范型
     * @param <RESPONSE> 执行策略出参范型
     */
    public <REQUEST, RESPONSE> RESPONSE chooseAndExecuteResp(String mark, REQUEST requestParam) {
        AbstractExecuteStrategy executeStrategy = choose(mark, null);
        return (RESPONSE) executeStrategy.executeResp(requestParam);
    }

    /**
     * 根据 mark 查询具体策略并执行，带返回结果
     * @param mark 策略标识
     * @param requestParam 执行策略入参
     * @param predicateFlag 匹配范解析标识
     * @return 执行策略出参范型
     * @param <REQUEST> 执行策略入参范型
     * @param <RESPONSE> 执行策略出参范型
     */
    public <REQUEST, RESPONSE> RESPONSE chooseAndExecuteResp(String mark, REQUEST requestParam, Boolean predicateFlag) {
        AbstractExecuteStrategy executeStrategy = choose(mark, predicateFlag);
        return (RESPONSE) executeStrategy.executeResp(requestParam);
    }

    @Override
    public void onApplicationEvent(ApplicationInitializingEvent event) {
        Map<String, AbstractExecuteStrategy> actual = ApplicationContextHolder.getBeansOfType(AbstractExecuteStrategy.class);
        actual.forEach((beanName,bean) -> {
            AbstractExecuteStrategy beanExist = abstractExecuteStrategyMap.get(bean.mark());
            if(beanExist != null){
                throw new ServiceException(String.format("[%s] Duplicate execution policy", bean.mark()));
            }
            abstractExecuteStrategyMap.put(bean.mark(),bean);
        });
    }
}
