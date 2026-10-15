package com.luoyx.lapis.uaa.config;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * 令牌下发处理器：把 Access Token / Refresh Token 写入 Cookie，而不是放进响应体。
 *
 * <p>安全加固说明：</p>
 * <ul>
 *     <li>Cookie 显式声明 {@code SameSite}，默认 {@code Lax}，阻断跨站请求携带令牌，
 *         这是"令牌走 Cookie"方案下的主要 CSRF 缓解手段；</li>
 *     <li>{@code Secure} 标记由配置 {@code lapis.security.cookie.secure} 控制，
 *         本地 HTTP 开发为 false，部署到 HTTPS 环境必须设为 true；</li>
 *     <li>令牌始终 {@code HttpOnly}，前端 JS 无法读取，规避 XSS 窃取令牌；</li>
 *     <li>Refresh Token 的 Cookie 路径收敛到 {@code /api/uaa/oauth2/token}，
 *         业务接口请求不会携带它，缩小泄露面；</li>
 *     <li>登录成功后主动下发 CSRF 令牌 Cookie，保证开启 CSRF 时前端一定能拿到令牌。</li>
 * </ul>
 *
 * @author Alan.Luo
 */
@Component
public class CookieTokenResponseHandler implements AuthenticationSuccessHandler {

    /**
     * Cookie 的 SameSite 策略（Strict / Lax / None）
     */
    @Value("${lapis.security.cookie.same-site:Lax}")
    private String cookieSameSite;

    /**
     * Cookie 是否仅通过 HTTPS 传输
     */
    @Value("${lapis.security.cookie.secure:false}")
    private boolean cookieSecure;

    /**
     * CSRF 令牌仓库。只有在 {@code lapis.security.csrf.enabled=true} 时才会有实例，
     * 因此使用 ObjectProvider 做可选注入，避免未开启 CSRF 时启动失败。
     */
    private final ObjectProvider<CsrfTokenRepository> csrfTokenRepositoryProvider;

    public CookieTokenResponseHandler(ObjectProvider<CsrfTokenRepository> csrfTokenRepositoryProvider) {
        this.csrfTokenRepositoryProvider = csrfTokenRepositoryProvider;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!(authentication instanceof OAuth2AccessTokenAuthenticationToken tokenAuthentication)) {
            return;
        }

        OAuth2AccessToken accessToken = tokenAuthentication.getAccessToken();
        OAuth2RefreshToken refreshToken = tokenAuthentication.getRefreshToken();

        // 将 Access Token 设置为 HttpOnly Cookie
        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", accessToken.getTokenValue())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(accessToken.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond())
                .build();

        // 将 Refresh Token 也设置为 Cookie，并把作用域收敛到刷新令牌端点
        ResponseCookie refreshTokenCookie = null;
        if (refreshToken != null) {
            refreshTokenCookie = ResponseCookie.from("refresh_token", refreshToken.getTokenValue())
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite(cookieSameSite)
                    .path("/api/uaa/oauth2/token")
                    .maxAge(Duration.ofDays(30))
                    .build();
        }

        response.setHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        if (refreshTokenCookie != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        }

        // 开启 CSRF 时，登录当下就把 CSRF 令牌下发给前端，避免首次写操作 403
        CsrfTokenRepository csrfTokenRepository = csrfTokenRepositoryProvider.getIfAvailable();
        if (csrfTokenRepository != null) {
            CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
            csrfTokenRepository.saveToken(csrfToken, request, response);
        }

        // 清空响应体
        response.setContentLength(0);
    }
}
