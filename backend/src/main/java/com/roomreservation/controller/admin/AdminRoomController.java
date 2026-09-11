package com.roomreservation.controller.admin;

import com.roomreservation.common.Result;
import com.roomreservation.config.interceptor.RequireRole;
import com.roomreservation.service.admin.AdminRoomService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端琴房接口：琴房增删改查与乐器明细整体覆盖
 */
@RestController
@RequestMapping("/api/admin/rooms")
/**
 * 管理端琴房接口：琴房增删改查与乐器明细维护，改动后清理相关缓存。
 */
public class AdminRoomController {

    @Resource
    private AdminRoomService adminRoomService;

    @RequireRole("admin")
    @GetMapping
    public Result list() {
        return Result.success(adminRoomService.listAll());
    }

    @RequireRole("admin")
    @PostMapping
    public Result create(@RequestBody Map<String, Object> body) {
        return Result.success(adminRoomService.create(body));
    }

    @RequireRole("admin")
    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        adminRoomService.update(id, body);
        return Result.success();
    }

    @RequireRole("admin")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        adminRoomService.delete(id);
        return Result.success();
    }
}
