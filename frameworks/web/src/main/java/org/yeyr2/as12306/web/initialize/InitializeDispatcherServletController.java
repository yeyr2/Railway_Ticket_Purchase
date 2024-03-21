package org.yeyr2.as12306.web.initialize;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.yeyr2.as12306.web.config.WebAutoConfiguration.INITIALIZE_PATH;

// 初始化DispatcherServlet类
@Slf4j(topic = "Initialize DispatcherServlet")
@RestController
public class InitializeDispatcherServletController {

    @GetMapping(INITIALIZE_PATH)
    public void initializeDispatcherServlet(){
        log.info("Initialized the dispatcherServlet to improve the first response time of the interface...");
    }
}
