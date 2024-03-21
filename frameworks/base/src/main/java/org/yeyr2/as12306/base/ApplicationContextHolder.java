package org.yeyr2.as12306.base;

import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * 应用程序上下文持有者
 */
public class ApplicationContextHolder implements ApplicationContextAware {

    @Getter
    private static ApplicationContext CONTEXT;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }

    public static <T> T getBean(Class<T> clazz) {
        return CONTEXT.getBean(clazz);
    }

    public static Object getBean(String name) {
        return CONTEXT.getBean(name);
    }

    public static <T> T getBean(Class<T> clazz,String name) {
        return CONTEXT.getBean(clazz,name);
    }

    public static <T> Map<String,T> getBeansOfType(Class<T> clazz) {
        return CONTEXT.getBeansOfType(clazz);
    }

    public static <A extends Annotation> A findAnnotationOnBean(String beanName,Class<A> annotationType) {
        return CONTEXT.findAnnotationOnBean(beanName,annotationType);
    }
}
