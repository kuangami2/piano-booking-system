package com.roomreservation.controller.admin;

import com.roomreservation.common.Result;
import com.roomreservation.config.interceptor.RequireRole;
import com.roomreservation.service.admin.AdminBookingService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端预约接口：按日期、琴房、用户、状态查询与管理员取消
 */
@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    @Resource
    private AdminBookingService adminBookingService;

    @RequireRole("admin")
    @GetMapping
    public Result list(@RequestParam(required = false) String date,
                       @RequestParam(required = false) Integer roomId,
                       @RequestParam(required = false) Integer userId,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(adminBookingService.page(date, roomId, userId, status, page, size));
    }

    @RequireRole("admin")
    @PutMapping("/{id}/cancel")
    public Result cancel(@PathVariable Integer id) {
        adminBookingService.cancel(id);
        return Result.success();
    }
}
