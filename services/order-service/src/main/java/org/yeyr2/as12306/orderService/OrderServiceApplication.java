package org.yeyr2.as12306.orderService;

import cn.crane4j.spring.boot.annotation.EnableCrane4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("org.yeyr2.as12306.orderService.dao.mapper")
@EnableFeignClients("org.yeyr2.as12306.orderService.remote")
@EnableCrane4j(enumPackages = "org.yey2.as12306.ticketService.common.enums")
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}