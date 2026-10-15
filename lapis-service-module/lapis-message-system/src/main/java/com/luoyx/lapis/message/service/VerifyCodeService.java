package com.luoyx.lapis.message.service;

import com.luoyx.lapis.message.support.VerifyCode;

/**
 * @author 罗英雄
 * @date 2021/9/12 14:48
 */
public interface VerifyCodeService {

    VerifyCode getCode();

    boolean check(String token, String code);
}
