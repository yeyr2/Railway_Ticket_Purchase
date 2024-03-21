package org.yeyr2.as12306.idempotent;

import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.bind.annotation.RequestParam;
import org.yeyr2.as12306.bizs.user.core.UserInfoDTO;
import org.yeyr2.as12306.cache.config.CacheAutoConfiguration;
import org.yeyr2.as12306.convention.result.Result;
import org.yeyr2.as12306.idempotent.annotation.Idempotent;
import org.yeyr2.as12306.idempotent.config.IdempotentAutoConfiguration;
import org.yeyr2.as12306.idempotent.core.IdempotentAspect;
import org.yeyr2.as12306.idempotent.core.spel.IdempotentSpELByRestAPIExecuteHandler;
import org.yeyr2.as12306.idempotent.core.token.IdempotentTokenExecuteHandler;
import org.yeyr2.as12306.idempotent.core.token.IdempotentTokenService;
import org.yeyr2.as12306.idempotent.enums.IdempotentSceneEnum;
import org.yeyr2.as12306.idempotent.enums.IdempotentTypeEnum;
import org.yeyr2.as12306.web.Results;

@SpringBootTest(classes = {RestApiSpELTestConfiguration.class,CacheAutoConfiguration.class,IdempotentAutoConfiguration.class})
@EnableAspectJAutoProxy
class IdempotentApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void RestApiSpELTest(){
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            // 设置需要注册的配置类
            context.register(RestApiSpELTestConfiguration.class);
            context.register(CacheAutoConfiguration.class);
            context.register(IdempotentAutoConfiguration.class);
            context.register(SpELTest.class);
            context.refresh();

            // 执行测试逻辑
            context.getBean(SpELTest.class).SpELKeyTest("wto");

        }

    }
}
