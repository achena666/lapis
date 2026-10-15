import {ApplicationConfig, importProvidersFrom, isDevMode} from '@angular/core';
import {provideRouter} from '@angular/router';

import {routes} from './app.routes';
import {provideAnimationsAsync} from '@angular/platform-browser/animations/async';
import {provideNzI18n, zh_CN} from 'ng-zorro-antd/i18n';
import {registerLocaleData} from '@angular/common';
import zh from '@angular/common/locales/zh';
import {FormsModule} from '@angular/forms';
import {
    provideHttpClient,
    withInterceptors,
    withXhr,
    withXsrfConfiguration
} from '@angular/common/http';
import {authInterceptor} from "./shared/auth/auth-interceptor-fn";

registerLocaleData(zh);

import {IconDefinition} from '@ant-design/icons-angular';

// 说明：这里依旧是"全量引入"，因为权限管理页的图标选择器（icon-picker）
// 需要 inject(NZ_ICONS) 拿到全部图标名供用户挑选，改成按需引入会直接破坏该功能。
// 优化点：只注册 outline 主题（图标选择器本身也只筛 outline），
//        相比原先 outline + fill + twotone 三套全注册，初始化开销约降到 1/3。
// 后续若要把图标改成 tree-shaking 的按需引入，需要先改造 icon-picker，
// 让它从一份静态图标名清单取数，而不是依赖 NZ_ICONS。
import * as AllIcons from '@ant-design/icons-angular/icons';
import {provideNzIcons} from "ng-zorro-antd/icon";
import {provideEffects} from '@ngrx/effects';
import {provideRouterStore} from '@ngrx/router-store';
import {provideStoreDevtools} from '@ngrx/store-devtools';
import {provideStore} from "@ngrx/store";
import {AuthEffects} from "./store/auth/auth.effect";
import {authReducer} from "./store/auth/auth.reducer";
import {provideNzDateFnsAdapter} from "ng-zorro-antd/core/time";
import {zhCN} from "date-fns/locale";

const antDesignIcons = AllIcons as {
    [key: string]: IconDefinition;
};

/**
 * 只注册 outline 主题的图标
 */
const icons: IconDefinition[] = Object.keys(antDesignIcons)
    .map(key => antDesignIcons[key])
    .filter(icon => icon != null && icon.theme === 'outline');

/**
 * CSRF 约定：Cookie 名 XSRF-TOKEN、请求头 X-XSRF-TOKEN。
 * 这里显式写出来，保证与后端 CookieCsrfTokenRepository 的默认约定一致
 * （后端开启 lapis.security.csrf.enabled=true 后可直接生效，前端无需改动）。
 */
const xsrf = withXsrfConfiguration({
    cookieName: 'XSRF-TOKEN',
    headerName: 'X-XSRF-TOKEN'
});

export const appConfig: ApplicationConfig = {
    providers: [
        // 路由配置 总入口
        provideRouter(routes),
        // 2026-10 修正：原先 provideAnimationsAsync() 被重复注册了两次
        provideAnimationsAsync(),
        provideNzI18n(zh_CN),
        provideNzDateFnsAdapter({
            locale: zhCN,
            firstDayOfWeek: 1
        }),
        importProvidersFrom(FormsModule),
        // 拦截器配置 总入口
        provideHttpClient(withXhr(), xsrf, withInterceptors([authInterceptor])),
        provideNzIcons(icons),
        // 状态管理（2026-10 修正：删除了 NgRx 脚手架自带的 counter 示例 reducer）
        provideEffects(AuthEffects),
        provideRouterStore(),
        provideStore({
            auth: authReducer
        }),
        // 2026-10 修正：devtools 只在开发模式注册，避免打进生产包
        ...(isDevMode() ? [provideStoreDevtools({maxAge: 25})] : [])
    ]
};
