package com.luoyx.lapis.admin.sys.service;

import com.luoyx.lapis.admin.api.sys.dto.SaveLoginHistoryDTO;
import com.luoyx.lapis.admin.sys.entity.LoginHistory;
import com.luoyx.lapis.mybatisplus.service.BaseService;

/**
 * <p>
 * 登录历史表 服务类
 * </p>
 *
 * @author LuoYingxiong
 * @since 2021-04-15
 */
public interface LoginHistoryService extends BaseService<LoginHistory> {

    /**
     * 保存登录日志
     *
     * @param saveLoginHistoryDTO 登录日志请求参数
     */
    void save(SaveLoginHistoryDTO saveLoginHistoryDTO);
}
