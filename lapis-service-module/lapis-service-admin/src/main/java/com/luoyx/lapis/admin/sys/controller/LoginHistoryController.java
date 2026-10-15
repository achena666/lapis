package com.luoyx.lapis.admin.sys.controller;


import com.luoyx.lapis.admin.api.sys.LoginHistoryAPI;
import com.luoyx.lapis.admin.api.sys.dto.SaveLoginHistoryDTO;
import com.luoyx.lapis.admin.sys.query.LoginHistoryQuery;
import com.luoyx.lapis.admin.sys.response.LoginHistoryVO;
import com.luoyx.lapis.admin.sys.service.LoginHistoryService;
import com.luoyx.lapis.mybatisplus.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 登录历史表 前端控制器
 * </p>
 *
 * @author LuoYingxiong
 * @since 2021-04-15
 */
@Tag(name = "登录历史")
@RequiredArgsConstructor
@RestController
@RequestMapping("/sys/login-histories")
public class LoginHistoryController implements LoginHistoryAPI {

    private final LoginHistoryService loginHistoryService;

    @PreAuthorize("hasAuthority('sys-login-history:list')")
    @GetMapping
    public PageResult<LoginHistoryVO> list(@ParameterObject @Validated LoginHistoryQuery loginHistoryQuery) throws Exception {
        return loginHistoryService.findPage(loginHistoryQuery);
    }

    @PreAuthorize("hasAuthority('sys-login-history:delete')")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @DeleteMapping(value = "/{id}")
    public void deleteById(@PathVariable("id") Long id) {
        // 2026-10 修正：去掉调试用的 System.out.println
        loginHistoryService.logicDeleteById(id);
    }
}

