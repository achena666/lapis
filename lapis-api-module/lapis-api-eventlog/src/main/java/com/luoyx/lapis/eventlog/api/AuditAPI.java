package com.luoyx.lapis.eventlog.api;

import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

public interface AuditAPI {

    @GetMapping("/testCallAudit")
    Map<String, String> testCallAudit();
}
