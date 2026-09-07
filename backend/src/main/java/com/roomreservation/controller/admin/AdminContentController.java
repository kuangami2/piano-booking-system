package com.roomreservation.controller.admin;

import com.roomreservation.common.Result;
import com.roomreservation.config.interceptor.RequireRole;
import com.roomreservation.service.admin.AdminContentService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端内容接口：轮播与公告增删改、图片上传
 */
@RestController
@RequestMapping("/api/admin")
public class AdminContentController {

    @Resource
    private AdminContentService adminContentService;

    @RequireRole("admin")
    @GetMapping("/banners")
    public Result banners() {
        return Result.success(adminContentService.banners());
    }

    @RequireRole("admin")
    @PostMapping("/banners")
    public Result addBanner(@RequestBody Map<String, Object> body) {
        return Result.success(adminContentService.addBanner(body));
    }

    @RequireRole("admin")
    @PutMapping("/banners/{id}")
    public Result updateBanner(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        adminContentService.updateBanner(id, body);
        return Result.success();
    }

    @RequireRole("admin")
    @DeleteMapping("/banners/{id}")
    public Result deleteBanner(@PathVariable Integer id) {
        adminContentService.deleteBanner(id);
        return Result.success();
    }

    @RequireRole("admin")
    @GetMapping("/notices")
    public Result notices() {
        return Result.success(adminContentService.notices());
    }

    @RequireRole("admin")
    @PostMapping("/notices")
    public Result addNotice(@RequestBody Map<String, Object> body) {
        return Result.success(adminContentService.addNotice(body));
    }

    @RequireRole("admin")
    @PutMapping("/notices/{id}")
    public Result updateNotice(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        adminContentService.updateNotice(id, body);
        return Result.success();
    }

    @RequireRole("admin")
    @DeleteMapping("/notices/{id}")
    public Result deleteNotice(@PathVariable Integer id) {
        adminContentService.deleteNotice(id);
        return Result.success();
    }

    @RequireRole("admin")
    @PostMapping("/upload/image")
    public Result uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> data = new HashMap<>();
        data.put("url", adminContentService.uploadImage(file));
        return Result.success(data);
    }
}
