import {MetaReducer} from '@ngrx/store';
import * as authReducer from './auth/auth.reducer';

// store中存储的状态类型接口
// 2026-10 修正：删除了 NgRx 脚手架自带的 counter 示例状态（全项目无任何组件使用）
export interface AppState {

    [authReducer.authFeatureKey]: authReducer.AuthState
}

// 状态名字和reducer的对应关系
export const reducers = {
    [authReducer.authFeatureKey]: authReducer.authReducer
};


export const metaReducers: MetaReducer<AppState>[] = [];
