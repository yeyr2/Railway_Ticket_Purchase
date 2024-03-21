package org.yeyr2.as12306.log.core;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.yeyr2.as12306.log.annotation.ILog;

import java.lang.reflect.Method;
import java.util.Optional;

// ILog 日志打印AOP切面
@Aspect
public class ILogPrintAspect {
    /**
     * 打印类或方法上的ILog
     */
    @Around("@within(org.yeyr2.as12306.log.annotation.ILog) || @annotation(org.yeyr2.as12306.log.annotation.ILog)")
    public Object printMLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = SystemClock.now();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Logger log = LoggerFactory.getLogger(methodSignature.getDeclaringTypeName());
        String beginTIme = DateUtil.now();
        Object result = null;
        try {
            result = joinPoint.proceed();
        }finally {
            Method targetMethod = joinPoint.getTarget().getClass().
                    getDeclaredMethod(methodSignature.getName(),methodSignature.getMethod().getParameterTypes());
            ILog logAnnotation = Optional.ofNullable(targetMethod.getAnnotation(ILog.class)).
                    orElse(joinPoint.getTarget().getClass().getAnnotation(ILog.class));
            if(logAnnotation != null){
                ILogPrintDTO logPrint = new ILogPrintDTO();
                logPrint.setBeginTime(beginTIme);
                if (logAnnotation.input()){
                    logPrint.setInputParams(buildInput(joinPoint));
                }
                if(logAnnotation.output()){
                    logPrint.setOutputParams(result);
                }
                String methodType = "";
                String requestURI = "";
                try{
                    ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    assert servletRequestAttributes != null;
                    methodType = servletRequestAttributes.getRequest().getMethod();
                    requestURI = servletRequestAttributes.getRequest().getRequestURI();
                }catch (Exception ignored){
                }
                log.info("[{}] {}, executeTime: {}ms, info: {}", methodType, requestURI, SystemClock.now() - startTime, JSON.toJSONString(logPrint));
            }
        }
        return result;
    }

    private Object[] buildInput(ProceedingJoinPoint joinPoint){
        Object[] args = joinPoint.getArgs();
        Object[] printArgs = new Object[args.length];
        for (int i = 0 ; i < args.length ; i++){
            if ( args[i] instanceof HttpServletRequest || args[i] instanceof HttpServletResponse){
                continue;
            }
            if(args[i] instanceof  byte[]){
                printArgs[i] = "byte array";
            }else if(args[i] instanceof MultipartFile){
                printArgs[i] = "file";
            }else {
                printArgs[i] = args[i];
            }
        }
        return printArgs;
    }
}
