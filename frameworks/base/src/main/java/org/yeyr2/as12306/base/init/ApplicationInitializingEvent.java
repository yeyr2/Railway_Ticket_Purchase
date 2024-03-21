package org.yeyr2.as12306.base.init;

import org.springframework.context.ApplicationEvent;

public class ApplicationInitializingEvent extends ApplicationEvent {
    public ApplicationInitializingEvent(Object source) {
        super(source);
    }
}
