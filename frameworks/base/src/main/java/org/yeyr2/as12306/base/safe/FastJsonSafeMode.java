package org.yeyr2.as12306.base.safe;

import org.springframework.beans.factory.InitializingBean;

// FastJson安全模式，关闭隐式的json转化，防止不安全的Json传入
public class FastJsonSafeMode implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        System.setProperty("fastjson2.parser.safeMode","true");
    }
}
