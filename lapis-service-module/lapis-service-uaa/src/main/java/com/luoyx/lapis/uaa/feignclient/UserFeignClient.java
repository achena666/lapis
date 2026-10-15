package com.luoyx.lapis.uaa.feignclient;

import com.luoyx.lapis.admin.api.sys.UserAPI;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "service-admin")
public interface UserFeignClient extends UserAPI {
}
