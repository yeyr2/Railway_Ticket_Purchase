package org.yeyr2.as12306.convention.page;

import lombok.Data;

/**
 * 分页请求对象   <p>
 * {@link PageRequest}、{@link PageResponse} 可以理解是防腐层的一种实现，不论底层 ORM 框架，对外分页参数属性不变
 */
@Data
public class PageRequest {
    // 当前页数
    private Long current = 1L;

    // 每页条数
    private Long size = 10L;
}
