package com.strider.feed.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import java.lang.annotation.Annotation;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
@Component
@Slf4j
public class LogAspect {
    // RestController가 붙은 클래스의 모든 public 메서드
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restControllerMethods() {}

    @Before("restControllerMethods()")
    public void logRequest(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        Annotation[][] paramAnnotations = signature.getMethod().getParameterAnnotations();
        StringBuilder headersLog = new StringBuilder();
        StringBuilder bodyLog = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            // HttpHeaders 파라미터
            if (arg instanceof HttpHeaders) {
                headersLog.append(arg.toString());
            }
            // @RequestBody 파라미터
            for (Annotation annotation : paramAnnotations[i]) {
                if (annotation.annotationType().equals(RequestBody.class)) {
                    bodyLog.append(arg);
                }
            }
        }
        log.info("[REQUEST] {}.{} Headers: {} Body: {}", joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(), headersLog, bodyLog);
    }

    @AfterReturning(pointcut = "restControllerMethods()", returning = "result")
    public void logResponse(JoinPoint joinPoint, Object result) {
        if (result instanceof ResponseEntity<?> response) {
            log.info("[RESPONSE] {}.{} Headers: {} Body: {}", joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    response.getHeaders(),
                    response.getBody());
        } else {
            log.info("[RESPONSE] {}.{} => {}", joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    result);
        }
    }
}
