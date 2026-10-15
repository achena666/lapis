package com.luoyx.lapis.uaa.feignclient;

import com.luoyx.lapis.admin.api.sys.LoginHistoryAPI;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "service-admin")
public interface LoginHistoryFeignClient extends LoginHistoryAPI {
}
