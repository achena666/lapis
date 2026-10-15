package com.luoyx.lapis.admin.sys.controller;

import com.luoyx.lapis.admin.util.LogicalDeleteTableDDLGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ToolController", description = "工具Controller")
@RestController
@RequestMapping(value = "/tool")
public class ToolController {

    /**
     * 生成逻辑删除归档表 DDL。
     *
     * <p>2026-10 修正：</p>
     * <ul>
     *     <li>补上权限控制：该接口会生成并执行建表语句，原先对任何登录用户开放，风险过高；</li>
     *     <li>方法可见性由 private 改为 public：Spring MVC 虽能通过反射调用 private 处理器方法，
     *         但属于非预期用法，且会让 AOP（含 @PreAuthorize 的方法级增强）行为变得不可靠。</li>
     * </ul>
     *
     * @param ddl 原始建表语句
     * @return 归档表建表语句
     */
    @Operation(summary = "生成逻辑删除归档表")
    @PreAuthorize("hasAuthority('sys-tool:generate-ddl')")
    @PostMapping(value = "/generate-deleted-table-ddl")
    public String generateDeletedTableDdl(@RequestBody String ddl) {
        return LogicalDeleteTableDDLGenerator.generateLogicalDeleteTableDDL(ddl);
    }
}
