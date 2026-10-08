package com.strider.feed.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignAuthInterceptorConfig {
    @Bean
    public RequestInterceptor authorizationInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 이 스레드가 처리 중인 Spring Web 요청에 대한 컨텍스트
                RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
                // servlet 기반 웹 요청인지 체크. 아니라면 헤더 추가 없이 return
                if(!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) return;
                // 실제로 들어온 HTTP 요청에서 Authorization header값 추출
                HttpServletRequest request = servletRequestAttributes.getRequest();
                String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
                // Authorization 값이 있다면 Feign 요청에 추가
                if(authHeader != null && !authHeader.isBlank()){
                    template.header(HttpHeaders.AUTHORIZATION, authHeader);
                }
            }
        };
    }
}
