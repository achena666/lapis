package com.luoyx.lapis.uaa.sys.service.impl;

import com.luoyx.lapis.admin.api.sys.dto.UserDTO;
import com.luoyx.lapis.admin.api.sys.query.LoginLookupQuery;
import com.luoyx.lapis.uaa.sys.converter.AuthorityConverter;
import com.luoyx.lapis.uaa.sys.converter.UserConverter;
import com.luoyx.lapis.uaa.feignclient.UserFeignClient;
import com.luoyx.lapis.framework.utils.SignUtils;
import com.luoyx.lapis.security.pojo.Menu;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 罗英雄
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserFeignClient userFeignClient;
    private final UserConverter userConverter;
    private final AuthorityConverter authorityConverter;

    @Value(value = "${login.lookup.secret}")
    private String loginLookupSecret;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDTO userDTO = userFeignClient.loginLookup(buildLoginLookupQuery(username));
        if (null == userDTO) {
            throw new UsernameNotFoundException("用户不存在");
        }
        log.info("正在登录的用户信息 => {}", userDTO);
        if (null != userDTO.getUserBaseInfo()) {
            final Long sysUserId = userDTO.getUserBaseInfo().getId();
            log.info("正在登录的用户id => {}", sysUserId);
        }

        List<SimpleGrantedAuthority> authorities = userDTO.getAuthorities().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        List<Menu> menuList = authorityConverter.toMenuList(userDTO.getSideMenuList());

        return userConverter.toCurrentSysUser(userDTO, authorities, menuList);
    }

    /**
     * 构建登录查询参数
     *
     * @param username 用户名
     * @return 登录查询参数
     */
    private LoginLookupQuery buildLoginLookupQuery(String username) {
        final long timestamp = System.currentTimeMillis();
        final String data = username + timestamp;

        return LoginLookupQuery.builder()
                .username(username)
                .timestamp(timestamp)
                .sign(SignUtils.hmacSHA256(loginLookupSecret, data))
                .build();
    }
}
