package com.peerlending.common.aspect;

import com.peerlending.common.annotation.LoggedUseCase;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class LoggedUseCaseAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggedUseCaseAspect.class);

    @Before("@annotation(com.peerlending.common.annotation.LoggedUseCase)")
    public void logUseCase(JoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = joinPoint.getTarget().getClass().getMethod(signature.getName(), signature.getParameterTypes());
        LoggedUseCase ann = method.getAnnotation(LoggedUseCase.class);
        String name = ann != null && !ann.value().isBlank() ? ann.value() : method.getName();
        log.info("Use case: {} ({})", name, joinPoint.getSignature().toShortString());
    }
}
