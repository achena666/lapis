package com.luoyx.lapis.security.autoconfigure;

import com.luoyx.lapis.security.interceptor.UserContextInterceptor;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 资源服务器通用安全配置。
 *
 * <p><b>关于 CSRF</b>：本项目把访问令牌放在 HttpOnly Cookie 中，属于"Cookie 承载凭据"的方案，
 * 理论上存在 CSRF 风险。当前默认不启用 CSRF，原因是登录端点 {@code /oauth2/token}
 * 本身就是"获取令牌之前"的首次写操作，开启后需要先解决令牌下发前无法校验的问题。
 * 因此采取的缓解措施是：令牌 Cookie 强制 {@code SameSite=Lax}
 * （见 {@code CookieTokenResponseHandler}），跨站点发起的写操作不会携带令牌，
 * 已能阻断绝大多数 CSRF 场景。
 *
 * <p>若部署到生产并希望彻底启用 CSRF，把 {@code lapis.security.csrf.enabled} 置为 {@code true} 即可。
 * 届时本配置会使用 {@link CookieCsrfTokenRepository}（{@code XSRF-TOKEN} Cookie +
 * {@code X-XSRF-TOKEN} 请求头，与 Angular HttpClient 的默认约定一致，前端无需改造），
 * 并在登录成功时主动下发 CSRF 令牌 Cookie（见 {@code CookieTokenResponseHandler}）。
 * 需要在 {@code lapis.security.csrf.ignore-paths} 中放行 {@code /oauth2/**} 等端点。
 * </p>
 *
 * @author Alan.Luo
 */
@Slf4j
@Configuration
public class SecurityAutoConfiguration implements WebMvcConfigurer {

    /**
     * 无需认证即可访问的路径，逗号分隔。
     *
     * <p>注意这里使用 {@code permitAll()} 而不是 {@code WebSecurity.ignoring()}：
     * ignoring() 会完全绕过 Spring Security 过滤器链（CSRF、安全响应头全部失效），
     * 而 permitAll() 仍然走完整过滤器链，只是不做鉴权判定，安全性更高。
     * </p>
     */
    @Value("${lapis.security.permit-paths:}")
    private String[] permitPaths;

    /**
     * 仅允许 POST 方法匿名访问的路径，逗号分隔。
     * 用于"只允许服务间写入、不允许匿名读取"的内部端点（例如登录历史上报），
     * 避免把整个路径放行后 GET 列表接口也被绕过鉴权。
     */
    @Value("${lapis.security.permit-post-paths:}")
    private String[] permitPostPaths;

    /**
     * 是否启用 CSRF 防护，默认关闭（原因见类注释）
     */
    @Value("${lapis.security.csrf.enabled:false}")
    private boolean csrfEnabled;

    /**
     * 启用 CSRF 时需要放行的路径，逗号分隔
     */
    @Value("${lapis.security.csrf.ignore-paths:/oauth2/**,/captchas/**}")
    private String[] csrfIgnorePaths;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new UserContextInterceptor())
                .addPathPatterns("/**");
    }

    /**
     * 把请求上下文中的 Cookie 透传给 Feign 调用，使服务间调用能延续用户身份。
     *
     * @return Feign 请求拦截器
     */
    @Bean
    public RequestInterceptor cookieRelayInterceptor() {
        log.info("开始设置Feign转发 登录凭据");
        return requestTemplate -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String cookieHeader = request.getHeader("Cookie");

                if (cookieHeader != null) {
                    requestTemplate.header("Cookie", cookieHeader);
                }
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   BearerTokenResolver cookieBearerTokenResolver,
                                                   OpaqueTokenIntrospector opaqueTokenIntrospector) throws Exception {
        log.info("security公共模块  执行资源服务器配置");

        http.authorizeHttpRequests(authorize -> {
            if (permitPaths != null && permitPaths.length > 0) {
                authorize.requestMatchers(permitPaths).permitAll();
            }
            if (permitPostPaths != null && permitPostPaths.length > 0) {
                authorize.requestMatchers(HttpMethod.POST, permitPostPaths).permitAll();
            }
            // 所有的访问都需要通过身份认证
            authorize.anyRequest().authenticated();
        });

        http.oauth2ResourceServer(oauth2 -> oauth2
                .opaqueToken(opaqueTokenConfigurer ->
                        opaqueTokenConfigurer.introspector(opaqueTokenIntrospector)
                )
                .bearerTokenResolver(cookieBearerTokenResolver)
                .authenticationEntryPoint(new MyAuthenticationEntryPoint())
        );

        if (csrfEnabled) {
            log.info("CSRF 防护已启用，放行路径：{}", String.join(", ", csrfIgnorePaths));
            http.csrf(csrf -> csrf
                    .csrfTokenRepository(csrfTokenRepository())
                    .ignoringRequestMatchers(csrfIgnorePaths)
            );
        } else {
            log.warn("CSRF 防护当前关闭（lapis.security.csrf.enabled=false）。"
                    + "令牌 Cookie 已强制 SameSite=Lax 作为缓解措施，生产环境建议开启。");
            http.csrf(AbstractHttpConfigurer::disable);
        }

        return http.build();
    }

    /**
     * 仅在开启 CSRF 时注册令牌仓库：Cookie 名为 {@code XSRF-TOKEN}、请求头为 {@code X-XSRF-TOKEN}，
     * 与 Angular HttpClient 的默认 XSRF 约定一致，前端无需额外改造。
     *
     * @return CSRF 令牌仓库
     */
    @Bean
    @ConditionalOnProperty(name = "lapis.security.csrf.enabled", havingValue = "true")
    public CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }


    @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.resourceserver.opaquetoken.introspection-uri}")
    private String introspectionUri;

    @Bean
    public OpaqueTokenIntrospector opaqueTokenIntrospector() {
        return new CustomOpaqueTokenIntrospector(introspectionUri, clientId, clientSecret);
    }

    /**
     * 从Cookie中提取访问令牌
     *
     * @return BearerTokenResolver
     */
    @Bean
    public BearerTokenResolver cookieBearerTokenResolver() {
        return new CookieBearerTokenResolver();
    }
}
