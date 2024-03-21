package org.yeyr2.as12306.web.initialize;


import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;

import static org.yeyr2.as12306.web.config.WebAutoConfiguration.INITIALIZE_PATH;

// 通过InitializeDispatcherServletController初始化DispatcherServlet
@RequiredArgsConstructor
public class InitializeDispatcherServletHandler implements CommandLineRunner {
    private final RestTemplate restTemplate;
    private final ConfigurableEnvironment configurableEnvironment;

    @Override
    public void run(String... args) throws Exception {
        String url = String.format("http://127.0.0.1:%s%s",
                configurableEnvironment.getProperty("server.port","8080") + configurableEnvironment.getProperty("server.servlet.context-path",""),
                INITIALIZE_PATH);
        try {
            restTemplate.execute(url, HttpMethod.GET,null,null);
        }catch (Throwable ignore){
        }
    }
}
