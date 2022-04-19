package com.example.mall.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class FeignConfig {

    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // 使用RequestContextHolder拿到刚进来的请求数据
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                // 老请求
                HttpServletRequest request = requestAttributes.getRequest();
                // 同步请求头的数据（主要是cookie）
                // 把老请求的cookie值放到新请求上来，进行一个同步
                String cookie = request.getHeader("Cookie");
                template.header("Cookie", cookie);
            }
        };
    }

}
