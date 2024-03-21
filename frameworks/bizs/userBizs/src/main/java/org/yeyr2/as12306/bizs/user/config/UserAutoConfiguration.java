package org.yeyr2.as12306.bizs.user.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.yeyr2.as12306.base.constant.FilterOrderConstant;
import org.yeyr2.as12306.base.constant.UserConstant;
import org.yeyr2.as12306.bizs.user.core.UserTransmitFilter;

// 用户配置自动装配
@ConditionalOnWebApplication
public class UserAutoConfiguration {

    // 用户信息传递过滤器
    public FilterRegistrationBean<UserTransmitFilter> globalUserTransmitFilter(){
        FilterRegistrationBean<UserTransmitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new UserTransmitFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(FilterOrderConstant.USER_TRANSMIT_FILTER_ORDER);
        return registration;
    }
}
