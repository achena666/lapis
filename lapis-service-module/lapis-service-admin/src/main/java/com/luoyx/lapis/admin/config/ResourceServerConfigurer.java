package com.luoyx.lapis.admin.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 管理服务（service-admin）的资源服务器配置。
 *
 * <p>安全加固说明（2026-10）：</p>
 * <ul>
 *     <li>原先这里用 {@code WebSecurity.ignoring()} 放行了 6 条业务路径。
 *         ignoring() 会<b>完全绕过 Spring Security 过滤器链</b>，
 *         导致这些接口连 CSRF、安全响应头都一并失效，且网关把 {@code /api/admin/**} 直接转发过来，
 *         等于对公网匿名开放。现已全部移除；</li>
 *     <li>经核对，其中 {@code /sys/users/find-by-username}、
 *         {@code /sys/authorities/findAuthoritiesByUserId}、{@code /sys/authorities/buildMenuTree}、
 *         {@code /userInfo/**}、{@code /EnumController/**} 这 5 条在代码中<b>根本没有对应端点</b>，
 *         属于历史遗留的"幽灵白名单"，直接删除；</li>
 *     <li>真正需要匿名访问的是 UAA 通过 Feign 调用的两个内部端点
 *         （{@code /sys/users/login-lookup} 与 POST {@code /sys/login-histories}），
 *         已改由配置中心的 {@code lapis.security.permit-paths} /
 *         {@code lapis.security.permit-post-paths} 以 {@code permitAll()} 方式放行，
 *         仍会经过完整过滤器链。</li>
 * </ul>
 *
 * @author 罗英雄
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true, securedEnabled = true)
public class ResourceServerConfigurer {

    /**
     * 仅对静态资源与接口文档放行。
     *
     * <p>这里只放"不涉及业务数据"的资源，业务接口的匿名访问一律走
     * {@code lapis.security.permit-paths} 配置，以保证过滤器链仍然生效。
     *
     * @return WebSecurityCustomizer
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web
                .ignoring()
                .requestMatchers(
                        "/swagger-ui.html", "/swagger-ui/**",
                        "/v3/api-docs/**", "/doc.html", "/webjars/**", "/favicon.ico"
                );
    }

    @Bean
    public PasswordEncoder bcryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
