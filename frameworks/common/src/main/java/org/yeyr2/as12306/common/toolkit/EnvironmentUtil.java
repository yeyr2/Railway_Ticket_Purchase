package org.yeyr2.as12306.common.toolkit;

import org.springframework.core.env.ConfigurableEnvironment;
import org.yeyr2.as12306.base.ApplicationContextHolder;

import java.util.ArrayList;
import java.util.List;

// 环境工具类
public class EnvironmentUtil {
    private static List<String> ENVIRONMENT_LIST = new ArrayList<>();

    static {
        ENVIRONMENT_LIST.add("dev");
        ENVIRONMENT_LIST.add("test");
    }

    // 判断是否为开发环境
    public static boolean isDevEnvironment(){
        ConfigurableEnvironment configurableEnvironment = ApplicationContextHolder.getBean(ConfigurableEnvironment.class);
        String propertyActive = configurableEnvironment.getProperty("spring.profiles.active","dev");
        return ENVIRONMENT_LIST.stream().anyMatch(propertyActive::contains);
    }

    // 判断当前是否为正式环境
    public static boolean isProdEnvironment(){
        ConfigurableEnvironment configurableEnvironment = ApplicationContextHolder.getBean(ConfigurableEnvironment.class);
        String propertyActive = configurableEnvironment.getProperty("spring.profiles.active","dev");
        return ENVIRONMENT_LIST.stream().noneMatch(propertyActive::contains);
    }
}
