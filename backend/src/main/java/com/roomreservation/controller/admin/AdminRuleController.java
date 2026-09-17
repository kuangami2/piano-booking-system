package com.roomreservation.controller.admin;

import com.roomreservation.common.Result;
import com.roomreservation.common.RuleCatalog;
import com.roomreservation.config.interceptor.RequireRole;
import com.roomreservation.service.admin.AdminRuleService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端规则接口：全量读取与批量更新，保存后即时生效
 */
@RestController
@RequestMapping("/api/admin/rules")
/**
 * 管理端规则接口：规则参数读取与批量更新，保存后刷新规则缓存即时生效。
 */
public class AdminRuleController {

    @Resource
    private AdminRuleService adminRuleService;

    @RequireRole("admin")
    @GetMapping
    public Result list() {
        return Result.success(adminRuleService.listAll());
    }

    /**
     * 参数元数据：合法区间与默认值，供管理端输入框约束与提示复用
     */
    @RequireRole("admin")
    @GetMapping("/meta")
    public Result meta() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (RuleCatalog.Meta meta : RuleCatalog.all()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", meta.key());
            item.put("min", meta.min());
            item.put("max", meta.max());
            item.put("defaultValue", meta.defaultValue());
            item.put("note", meta.note());
            list.add(item);
        }
        return Result.success(list);
    }

    @RequireRole("admin")
    @PutMapping
    public Result batchUpdate(@RequestBody List<Map<String, Object>> rules) {
        return Result.success(adminRuleService.batchUpdate(rules));
    }
}
