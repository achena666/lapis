package com.luoyx.lapis.uaa.feignclient;

import com.luoyx.lapis.eventlog.api.AuditAPI;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "service-audit")
public interface AuditFeignClient extends AuditAPI {


}
