package org.yeyr2.as12306.base.init;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.atomic.AtomicBoolean;

public class ApplicationContentPostProcessor implements ApplicationListener<ApplicationReadyEvent> {

    private final ApplicationContext applicationContext;

    // 保证之只执行一次
    private final AtomicBoolean onlyOnce = new AtomicBoolean(true);

    public ApplicationContentPostProcessor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if(onlyOnce.compareAndSet(true,false)){
            applicationContext.publishEvent(new ApplicationInitializingEvent(this));
        }
    }
}
