package org.example.gwtzsc.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import org.example.gwtzsc.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${upload.path}")
    private String uploadPath;

    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("仅支持图片文件上传");
        }
        String originalName = file.getOriginalFilename();
        String ext = FileUtil.extName(originalName);
        if (!isAllowedExt(ext)) {
            return Result.error("仅支持 jpg/png/gif/webp 格式");
        }
        String newName = IdUtil.fastSimpleUUID() + "." + ext;
        File dest = new File(uploadPath, newName);
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            return Result.error("上传失败");
        }
        String url = "/uploads/" + newName;
        return Result.success(Map.of("url", url));
    }

    @PostMapping("/images")
    public Result<List<Map<String, String>>> uploadImages(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Result.error("请选择至少一张图片");
        }
        List<Map<String, String>> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) continue;
            String ext = FileUtil.extName(file.getOriginalFilename());
            if (!isAllowedExt(ext)) continue;
            String newName = IdUtil.fastSimpleUUID() + "." + ext;
            File dest = new File(uploadPath, newName);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            try {
                file.transferTo(dest);
            } catch (IOException e) {
                continue;
            }
            String url = "/uploads/" + newName;
            urls.add(Map.of("url", url));
        }
        return Result.success(urls);
    }

    private boolean isAllowedExt(String ext) {
        if (ext == null) return false;
        ext = ext.toLowerCase();
        return "jpg".equals(ext) || "jpeg".equals(ext) ||
               "png".equals(ext) || "gif".equals(ext) || "webp".equals(ext);
    }
}
