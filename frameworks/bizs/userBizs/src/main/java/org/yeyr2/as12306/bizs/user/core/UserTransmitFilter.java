package org.yeyr2.as12306.bizs.user.core;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.yeyr2.as12306.base.constant.UserConstant;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

// 用户信息传递过滤器
public class UserTransmitFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;
        String userId = httpServletRequest.getHeader(UserConstant.USER_ID);
        if(StringUtils.hasText(userId)){
            String username = httpServletRequest.getHeader(UserConstant.USERNAME);
            String realName = httpServletRequest.getHeader(UserConstant.USER_REAL_NAME);
            if(StringUtils.hasText(username)){
                username = URLDecoder.decode(username, StandardCharsets.UTF_8);
            }
            if(StringUtils.hasText(realName)){
                realName = URLDecoder.decode(realName,StandardCharsets.UTF_8);
            }
            String token = httpServletRequest.getHeader(UserConstant.USER_TOKEN);
            UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                    .userId(userId)
                    .username(username)
                    .realName(realName)
                    .token(token)
                    .build();
            UserContext.setUser(userInfoDTO);
        }
        try{
            filterChain.doFilter(servletRequest,servletResponse);
        }finally {
            UserContext.removeUserContent();
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
