package org.yey2.as12306.ticketService;

import cn.crane4j.spring.boot.annotation.EnableCrane4j;
import cn.hippo4j.core.enable.EnableDynamicThreadPool;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

@EnableDynamicThreadPool
@SpringBootApplication(scanBasePackages = {
        "org.yeyr2.as12306.userService",
        "org.yeyr2.as12306.ticketService",
        "org.yeyr2.as12306.orderService",
        "org.yeyr2.as12306.payService"
})
@EnableRetry
@MapperScan(value = {
        "org.yeyr2.as12306.userService.dao.mapper",
        "org.yeyr2.as12306.ticketService.dao.mapper",
        "org.yeyr2.as12306.orderService.dao.mapper",
        "org.yeyr2.as12306.payService.dao.mapper"
})
@EnableFeignClients(value = {
        "org.yeyr2.as12306.ticketService.remote",
        "org.yeyr2.as12306.orderService.remote"
})
@EnableCrane4j(enumPackages = "org.opengoofy.index12306.biz.orderservice.common.enums")
public class AggregationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AggregationServiceApplication.class, args);
    }
}