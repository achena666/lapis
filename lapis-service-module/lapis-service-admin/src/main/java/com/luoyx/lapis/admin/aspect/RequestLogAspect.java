package com.luoyx.lapis.admin.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 日志打印切面。
 *
 * <p>安全与性能加固说明（2026-10）：</p>
 * <ul>
 *     <li>原先以 INFO 级别把<b>完整的请求参数和响应体序列化后落盘</b>，
 *         登录、改密、重置密码等接口的口令会明文进入日志文件，属于高危泄露；
 *         现在对敏感字段做脱敏，并且不在切面上输出响应体；</li>
 *     <li>日志级别降为 DEBUG，生产环境默认不输出，避免高频序列化带来的性能损耗
 *         （原先每个请求都要对入参和出参各做一次 JSON 序列化）；</li>
 *     <li>去掉了原先把日志"刷屏"的超长分隔线常量。</li>
 * </ul>
 *
 * @author 罗英雄
 */
@Slf4j
@Component
@Aspect
@Order(100)
@RequiredArgsConstructor
public class RequestLogAspect {

    /**
     * 需要脱敏的参数名（不区分大小写）
     */
    private static final Set<String> SENSITIVE_PARAM_NAMES = new HashSet<>(Arrays.asList(
            "password", "oldpassword", "newpassword", "confirmpassword",
            "secret", "clientsecret",
            "token", "accesstoken", "refreshtoken", "authorization",
            "captcha", "captchacode", "verifycode"
    ));

    /**
     * 脱敏占位符
     */
    private static final String MASK = "******";

    private final ObjectMapper objectMapper;

    @Pointcut("execution(* com.luoyx.lapis.admin.*.controller..*(..))")
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
     * 对参数做脱敏后再序列化：命中敏感字段名的直接替换为掩码，
     * 其余对象按 Jackson 默认序列化（序列化失败时退化为 toString，避免影响主流程）。
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
