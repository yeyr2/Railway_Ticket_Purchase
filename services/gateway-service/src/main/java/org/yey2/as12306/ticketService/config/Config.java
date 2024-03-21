package org.yey2.as12306.ticketService.config;

import lombok.Data;

import java.util.List;

@Data
public class Config {

    /**
     * 黑名单前置路径
     */
    private List<String> blackPathPre;
}
