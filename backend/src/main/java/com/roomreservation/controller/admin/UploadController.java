package com.roomreservation.controller.admin;

import com.roomreservation.common.Constants;
import com.roomreservation.config.interceptor.AuthAccess;
import com.roomreservation.exception.ServiceException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 上传图片公开访问接口，文件保存在运行目录 uploads/ 下
 */
@RestController
@RequestMapping("/api/uploads")
/**
 * 上传文件公开访问接口：按文件名读取运行目录 uploads 下的图片。
 */
public class UploadController {

    @AuthAccess
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> file(@PathVariable String filename) {
        if (filename == null || !filename.matches("[A-Za-z0-9._-]+")) {
            throw new ServiceException(Constants.CODE_400, "文件名不合法");
        }
        Path path = Paths.get("uploads").resolve(filename).normalize();
        if (!Files.isRegularFile(path)) {
            throw new ServiceException(Constants.CODE_404, "文件不存在");
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(filename)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        FileSystemResource resource = new FileSystemResource(path.toFile());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header("Cache-Control", "max-age=3600")
                .body(resource);
    }
}
