package com.luoyx.lapis.admin.sys.feignclient;

import com.luoyx.lapis.uaa.api.UAAAPI;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "service-uaa")
public interface UAAFeignClient extends UAAAPI {


}
