package com.roomreservation.service.admin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.roomreservation.common.Constants;
import com.roomreservation.entity.Banner;
import com.roomreservation.entity.Notice;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.mapper.BannerMapper;
import com.roomreservation.mapper.NoticeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 管理端内容 Service：轮播与公告增删改、图片上传
 */
@Service
/**
 * 管理端内容业务：公告与轮播维护、图片保存。
 */
public class AdminContentService {

    private static final Set<String> IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    @Resource
    private BannerMapper bannerMapper;
    @Resource
    private NoticeMapper noticeMapper;

    // ---------- 轮播 ----------

    public List<Banner> banners() {
        return bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .orderByAsc(Banner::getSort).orderByAsc(Banner::getId));
    }

    public Banner addBanner(Map<String, Object> body) {
        Banner banner = new Banner();
        String image = body.get("image") == null ? null : body.get("image").toString().trim();
        if (StrUtil.isBlank(image)) {
            throw new ServiceException(Constants.CODE_400, "图片地址不能为空");
        }
        banner.setImage(image);
        Object link = body.get("link");
        banner.setLink(link == null ? "" : link.toString().trim());
        banner.setSort(parseInt(body.get("sort"), "sort", 0));
        Object status = body.get("status");
        banner.setStatus(status == null ? "normal" : status.toString());
        checkStatus(banner.getStatus());
        bannerMapper.insert(banner);
        return banner;
    }

    public void updateBanner(Integer id, Map<String, Object> body) {
        requireBanner(id);
        LambdaUpdateWrapper<Banner> wrapper = new LambdaUpdateWrapper<Banner>().eq(Banner::getId, id);
        if (body.containsKey("image")) {
            String image = body.get("image") == null ? null : body.get("image").toString().trim();
            if (StrUtil.isBlank(image)) {
                throw new ServiceException(Constants.CODE_400, "图片地址不能为空");
            }
            wrapper.set(Banner::getImage, image);
        }
        if (body.containsKey("link")) {
            Object link = body.get("link");
            wrapper.set(Banner::getLink, link == null ? "" : link.toString().trim());
        }
        if (body.containsKey("sort")) {
            wrapper.set(Banner::getSort, parseInt(body.get("sort"), "sort", null));
        }
        if (body.containsKey("status")) {
            Object status = body.get("status");
            String statusStr = status == null ? null : status.toString();
            checkStatus(statusStr);
            wrapper.set(Banner::getStatus, statusStr);
        }
        if (wrapper.getSqlSet().isEmpty()) {
            throw new ServiceException(Constants.CODE_400, "没有可更新的字段");
        }
        bannerMapper.update(null, wrapper);
    }

    public void deleteBanner(Integer id) {
        requireBanner(id);
        bannerMapper.deleteById(id);
    }

    // ---------- 公告 ----------

    public List<Notice> notices() {
        return noticeMapper.selectList(new LambdaQueryWrapper<Notice>().orderByDesc(Notice::getId));
    }

    public Notice addNotice(Map<String, Object> body) {
        Notice notice = new Notice();
        String title = body.get("title") == null ? null : body.get("title").toString().trim();
        if (StrUtil.isBlank(title)) {
            throw new ServiceException(Constants.CODE_400, "公告标题不能为空");
        }
        notice.setTitle(title);
        Object content = body.get("content");
        notice.setContent(content == null ? null : content.toString().trim());
        Object status = body.get("status");
        notice.setStatus(status == null ? "normal" : status.toString());
        checkStatus(notice.getStatus());
        noticeMapper.insert(notice);
        return notice;
    }

    public void updateNotice(Integer id, Map<String, Object> body) {
        requireNotice(id);
        LambdaUpdateWrapper<Notice> wrapper = new LambdaUpdateWrapper<Notice>().eq(Notice::getId, id);
        if (body.containsKey("title")) {
            String title = body.get("title") == null ? null : body.get("title").toString().trim();
            if (StrUtil.isBlank(title)) {
                throw new ServiceException(Constants.CODE_400, "公告标题不能为空");
            }
            wrapper.set(Notice::getTitle, title);
        }
        if (body.containsKey("content")) {
            Object content = body.get("content");
            wrapper.set(Notice::getContent, content == null ? null : content.toString().trim());
        }
        if (body.containsKey("status")) {
            Object status = body.get("status");
            String statusStr = status == null ? null : status.toString();
            checkStatus(statusStr);
            wrapper.set(Notice::getStatus, statusStr);
        }
        if (wrapper.getSqlSet().isEmpty()) {
            throw new ServiceException(Constants.CODE_400, "没有可更新的字段");
        }
        noticeMapper.update(null, wrapper);
    }

    public void deleteNotice(Integer id) {
        requireNotice(id);
        noticeMapper.deleteById(id);
    }

    // ---------- 图片上传 ----------

    /**
     * 保存上传图片到运行目录 uploads/，返回可访问的相对地址 /api/uploads/{文件名}
     */
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(Constants.CODE_400, "请选择要上传的图片");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase();
        if (!IMAGE_EXTS.contains(ext)) {
            throw new ServiceException(Constants.CODE_400, "仅支持 jpg、png、gif、webp 图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ServiceException(Constants.CODE_400, "图片大小不能超过 5MB");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Path dir = Paths.get("uploads");
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ServiceException(Constants.CODE_500, "图片保存失败：" + e.getMessage());
        }
        return "/api/uploads/" + filename;
    }

    private void requireBanner(Integer id) {
        if (id == null || bannerMapper.selectById(id) == null) {
            throw new ServiceException(Constants.CODE_404, "轮播图不存在");
        }
    }

    private void requireNotice(Integer id) {
        if (id == null || noticeMapper.selectById(id) == null) {
            throw new ServiceException(Constants.CODE_404, "公告不存在");
        }
    }

    private void checkStatus(String status) {
        if (!"normal".equals(status) && !"disabled".equals(status)) {
            throw new ServiceException(Constants.CODE_400, "状态只能为 normal 或 disabled");
        }
    }

    private Integer parseInt(Object value, String field, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new ServiceException(Constants.CODE_400, field + " 应为整数");
        }
    }
}
