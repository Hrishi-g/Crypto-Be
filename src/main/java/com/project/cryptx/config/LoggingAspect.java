package com.project.cryptx.config;

import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("within(com.project.cryptx.controller..*) || within(com.project.cryptx.service..*)")
    public void applicationPackagePointcut() {
    }

    @Around("applicationPackagePointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        
        log.info("Enter: {} with argument[s] = {}", methodName, Arrays.toString(args));
        
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            
            if (result instanceof Mono) {
                return ((Mono<?>) result)
                        .doOnSuccess(val -> {
                            long duration = System.currentTimeMillis() - start;
                            log.info("Exit (Async Mono): {} returning = {} (took {} ms)", methodName, val, duration);
                        })
                        .doOnError(err -> {
                            log.error("Exception (Async Mono) in {}: {}", methodName, err.getMessage());
                        });
            } else if (result instanceof Flux) {
                return ((Flux<?>) result)
                        .doOnComplete(() -> {
                            long duration = System.currentTimeMillis() - start;
                            log.info("Exit (Async Flux): {} completed successfully (took {} ms)", methodName, duration);
                        })
                        .doOnError(err -> {
                            log.error("Exception (Async Flux) in {}: {}", methodName, err.getMessage());
                        });
            } else {
                long executionTime = System.currentTimeMillis() - start;
                log.info("Exit: {} returning = {} (took {} ms)", methodName, result, executionTime);
                return result;
            }
        } catch (Throwable e) {
            log.error("Exception in {}: {}", methodName, e.getMessage());
            throw e;
        }
    }
}
