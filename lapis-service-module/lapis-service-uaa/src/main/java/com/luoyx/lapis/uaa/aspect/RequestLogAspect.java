package com.luoyx.lapis.uaa.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 日志打印切面。
 *
 * <p>安全加固说明（2026-10）：本切面位于<b>认证服务</b>，原实现以 INFO 级别完整打印
 * 请求参数与响应体，登录接口的 username/password、验证码都会被明文写入日志文件，
 * 属于高危泄露。现已改为：</p>
 * <ul>
 *     <li>敏感字段（password / captcha / token / secret 等）统一脱敏为 {@code ******}；</li>
 *     <li>不再打印响应体；</li>
 *     <li>日志级别降为 DEBUG，生产环境默认不输出。</li>
 * </ul>
 *
 * @author 罗英雄
 */
@Slf4j
@Component
@Aspect
@RequiredArgsConstructor
public class RequestLogAspect {

    /**
     * 需要脱敏的参数名（不区分大小写）
     */
    private static final Set<String> SENSITIVE_PARAM_NAMES = new HashSet<>(Arrays.asList(
            "password", "oldpassword", "newpassword", "confirmpassword",
            "secret", "clientsecret",
            "token", "accesstoken", "refreshtoken", "authorization",
            "captcha", "captchacode", "verifycode",
            "username"
    ));

    /**
     * 脱敏占位符
     */
    private static final String MASK = "******";

    private final ObjectMapper objectMapper;

    @Pointcut("execution(* com.luoyx.lapis.uaa.controller..*(..))")
    public void requestServer() {
    }

    /**
     * 环绕通知
     *
     * @param proceedingJoinPoint 连接点
     * @return 目标方法返回值
     * @throws Throwable 目标方法抛出的异常
     */
    @Around("requestServer()")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = proceedingJoinPoint.proceed();

        if (log.isDebugEnabled()) {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes == null ? null : attributes.getRequest();

            log.debug("\n[IP]\t\t==> {}\t[HTTP方法] ==> {}\t[URL] ==> {}\n[类方法]\t\t==> {}\n[请求参数]\t==> {}\n[耗时]\t\t==> {}ms",
                    request == null ? "-" : request.getRemoteAddr(),
                    request == null ? "-" : request.getMethod(),
                    request == null ? "-" : request.getRequestURL(),
                    String.format("%s.%s", proceedingJoinPoint.getSignature().getDeclaringTypeName(),
                            proceedingJoinPoint.getSignature().getName()),
                    maskAndSerialize(getRequestParamsByProceedingJoinPoint(proceedingJoinPoint)),
                    (System.currentTimeMillis() - start));
        }

        return result;
    }

    @AfterThrowing(pointcut = "requestServer()", throwing = "e")
    public void doAfterThrow(JoinPoint joinPoint, RuntimeException e) throws JsonProcessingException {
        if (!log.isDebugEnabled()) {
            return;
        }
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();

        log.debug("\n异常\n[IP]\t\t==> {}\n[URL]\t\t==> {}\n[HTTP方法]\t==> {}\n[类方法]\t\t==> {}\n[请求参数]\t==> {}\n[异常信息]\t==> {}",
                request == null ? "-" : request.getRemoteAddr(),
                request == null ? "-" : request.getRequestURL(),
                request == null ? "-" : request.getMethod(),
                String.format("%s.%s", joinPoint.getSignature().getDeclaringTypeName(),
                        joinPoint.getSignature().getName()),
                maskAndSerialize(getRequestParamsByJoinPoint(joinPoint)),
                e.getMessage());
    }

    /**
     * 获取入参
     *
     * @param proceedingJoinPoint 连接点
     * @return 参数名与参数值的映射
     */
    private Map<String, Object> getRequestParamsByProceedingJoinPoint(ProceedingJoinPoint proceedingJoinPoint) {
        //参数名
        String[] paramNames = ((MethodSignature) proceedingJoinPoint.getSignature()).getParameterNames();
        //参数值
        Object[] paramValues = proceedingJoinPoint.getArgs();

        return buildRequestParam(paramNames, paramValues);
    }

    private Map<String, Object> getRequestParamsByJoinPoint(JoinPoint joinPoint) {
        // 参数名
        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        // 参数值
        Object[] paramValues = joinPoint.getArgs();

        return buildRequestParam(paramNames, paramValues);
    }

    private Map<String, Object> buildRequestParam(String[] paramNames, Object[] paramValues) {
        Map<String, Object> requestParams = new HashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            Object value = paramValues[i];

            //如果是文件对象
            if (value instanceof MultipartFile) {
                MultipartFile file = (MultipartFile) value;

                //获取文件名
                value = file.getOriginalFilename();
            } else if (value instanceof BeanPropertyBindingResult) {
                continue;
            }

            requestParams.put(paramNames[i], value);
        }

        return requestParams;
    }

    /**
     * 对参数做脱敏后再序列化。
     *
     * @param params 原始参数
     * @return 脱敏后的字符串
     */
    private String maskAndSerialize(Map<String, Object> params) {
        Map<String, Object> masked = new HashMap<>(params.size());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            if (key != null && SENSITIVE_PARAM_NAMES.contains(key.toLowerCase(Locale.ROOT))) {
                masked.put(key, MASK);
            } else {
                masked.put(key, entry.getValue());
            }
        }

        try {
            return objectMapper.writeValueAsString(masked);
        } catch (JsonProcessingException e) {
            // 序列化失败不应影响业务请求
            return String.valueOf(masked);
        }
    }
}
