ackage yuan.demo.controller;

import com.drew.imaging.ImageProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import yuan.demo.dto.ExifInfo;
import yuan.demo.service.ExifExtractorService;
import yuan.demo.service.PosterRenderService;
import yuan.demo.template.PosterTemplate;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 图片上传控制器（海报生成版）
 * 
 * 提供以下功能：
 * 1. 单张图片上传并提取 EXIF 信息
 * 2. 批量图片上传并打包 ZIP 返回
 * 3. 海报预览页面
 * 4. 高质量海报图片下载
 */
@Slf4j
@Controller
@RequestMapping("/api/image")
@RequiredArgsConstructor
public class ImageUploadController {

        private final ExifExtractorService exifExtractorService;
        private final PosterRenderService posterRenderService;

        /**
         * 处理单张图片上传请求（跳转到预览页面）
         */
        @PostMapping("/upload")
        public String uploadImage(
                        @RequestParam("image") MultipartFile file,
                        @RequestParam(value = "template", defaultValue = "classic") String templateId,
                        Model model,
                        RedirectAttributes redirectAttributes) {

                log.info("接收到图片上传请求，文件名: {}, 大小: {} 字节, 模板: {}",
                                file.getOriginalFilename(), file.getSize(), templateId);

                if (file.isEmpty()) {
                        redirectAttributes.addFlashAttribute("error", "请选择要上传的图片文件");
                        return "redirect:/";
                }

                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                        redirectAttributes.addFlashAttribute("error", "请上传有效的图片文件");
                        return "redirect:/";
                }

                try {
                        ExifInfo exifInfo = exifExtractorService.extractExifInfo(file);
                        List<PosterTemplate> templates = posterRenderService.getAllTemplates();

                        model.addAttribute("exif", exifInfo);
                        model.addAttribute("templates", templates);
                        model.addAttribute("currentTemplateId", templateId);

                        log.info("EXIF 信息提取成功，跳转到海报预览页面");
                        return "poster";

                } catch (ImageProcessingException e) {
                        log.error("图片处理失败: {}", e.getMessage(), e);
                        redirectAttributes.addFlashAttribute("error", "图片处理失败：" + e.getMessage());
                        return "redirect:/";
                } catch (IOException e) {
                        log.error("文件读取失败: {}", e.getMessage(), e);
                        redirectAttributes.addFlashAttribute("error", "服务器读取文件时发生错误");
                        return "redirect:/";
                }
        }

        /**
         * 批量上传图片并生成海报 ZIP 包
         * 
         * @param files    多个图片文件
         * @param template 模板 ID
         * @param format   导出格式（jpg/png）
         * @return ZIP 压缩包
         */
        @PostMapping("/batch")
        @ResponseBody
        public ResponseEntity<byte[]> batchProcess(
                        @RequestParam("images") MultipartFile[] files,
                        @RequestParam(value = "template", defaultValue = "classic") String template,
                        @RequestParam(value = "format", defaultValue = "jpg") String format) {

                log.info("接收到批量处理请求，共 {} 张图片，模板: {}, 格式: {}", files.length, template, format);

                if (files.length == 0) {
                        return ResponseEntity.badRequest().build();
                }

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ZipOutputStream zos = new ZipOutputStream(baos)) {

                        int successCount = 0;
                        boolean isJpeg = "jpg".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format);
                        String extension = isJpeg ? ".jpg" : ".png";

                        for (int i = 0; i < files.length; i++) {
                                MultipartFile file = files[i];

                                if (file.isEmpty()) {
                                        log.warn("文件 {} 为空，跳过", i);
                                        continue;
                                }

                                try {
                                        // 提取 EXIF 信息
                                        ExifInfo exifInfo = exifExtractorService.extractExifInfo(file);

                                        // 加载图片（只加载一次，loadImageFromBase64 已包含 EXIF 方向校正）
                                        BufferedImage originalImage = posterRenderService
                                                        .loadImageFromBase64(exifInfo.getImageBase64());

                                        // 渲染海报
                                        BufferedImage poster = posterRenderService.renderPoster(template, originalImage,
                                                        exifInfo);

                                        // 导出图片
                                        byte[] imageData = isJpeg
                                                        ? posterRenderService.exportAsJpeg(poster, 0.9f)
                                                        : posterRenderService.exportAsPng(poster);

                                        // 生成文件名
                                        String originalName = file.getOriginalFilename();
                                        String baseName = originalName != null
                                                        ? originalName.replaceAll("\\.[^.]+$", "")
                                                        : "poster_" + (i + 1);
                                        String zipEntryName = baseName + "_poster" + extension;

                                        // 添加到 ZIP
                                        zos.putNextEntry(new ZipEntry(zipEntryName));
                                        zos.write(imageData);
                                        zos.closeEntry();

                                        successCount++;
                                        log.info("处理完成 {}/{}: {}", successCount, files.length, zipEntryName);

                                        // 释放内存
                                        poster.flush();
                                        originalImage.flush();

                                } catch (Exception e) {
                                        log.error("处理文件 {} 失败: {}", file.getOriginalFilename(), e.getMessage());
                                        // 继续处理其他文件
                                }
                        }

                        if (successCount == 0) {
                                return ResponseEntity.badRequest().build();
                        }

                        zos.finish();
                        byte[] zipData = baos.toByteArray();

                        String filename = "posters_" +
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                                        + ".zip";

                        log.info("批量处理完成，成功 {}/{} 张，ZIP 大小: {} KB",
                                        successCount, files.length, zipData.length / 1024);

                        return ResponseEntity.ok()
                                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                                        "attachment; filename=\"" + filename + "\"")
                                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                        .body(zipData);

                } catch (IOException e) {
                        log.error("批量处理失败: {}", e.getMessage(), e);
                        return ResponseEntity.internalServerError().build();
                }
        }

        /**
         * 下载单张渲染后的海报图片
         */
        @PostMapping("/download")
        @ResponseBody
        public ResponseEntity<byte[]> downloadPoster(
                        @RequestParam("template") String templateId,
                        @RequestParam("imageBase64") String imageBase64,
                        @RequestParam("cameraModel") String cameraModel,
                        @RequestParam("lensModel") String lensModel,
                        @RequestParam("focalLength") String focalLength,
                        @RequestParam("aperture") String aperture,
                        @RequestParam("shutterSpeed") String shutterSpeed,
                        @RequestParam("iso") String iso,
                        @RequestParam(value = "format", defaultValue = "png") String format) {

                log.info("接收到海报下载请求，模板: {}, 格式: {}", templateId, format);

                BufferedImage originalImage = null;
                BufferedImage poster = null;

                try {
                        originalImage = posterRenderService.loadImageFromBase64(imageBase64);

                        ExifInfo exifInfo = ExifInfo.builder()
                                        .cameraModel(cameraModel)
                                        .lensModel(lensModel)
                                        .focalLength(focalLength)
                                        .aperture(aperture)
                                        .shutterSpeed(shutterSpeed)
                                        .iso(iso)
                                        .build();

                        poster = posterRenderService.renderPoster(templateId, originalImage, exifInfo);

                        byte[] imageData;
                        String extension;
                        MediaType mediaType;

                        if ("jpg".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format)) {
                                imageData = posterRenderService.exportAsJpeg(poster, 0.9f);
                                extension = ".jpg";
                                mediaType = MediaType.IMAGE_JPEG;
                        } else {
                                imageData = posterRenderService.exportAsPng(poster);
                                extension = ".png";
                                mediaType = MediaType.IMAGE_PNG;
                        }

                        String filename = "poster_" +
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                                        extension;

                        log.info("海报渲染完成，格式: {}, 文件大小: {} KB", format, imageData.length / 1024);

                        return ResponseEntity.ok()
                                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                                        "attachment; filename=\"" + filename + "\"")
                                        .contentType(mediaType)
                                        .body(imageData);

                } catch (IOException e) {
                        log.error("海报渲染失败: {}", e.getMessage(), e);
                        return ResponseEntity.internalServerError().build();
                } finally {
                        // 释放内存，避免内存泄漏
                        if (originalImage != null) {
                                originalImage.flush();
                        }
                        if (poster != null) {
                                poster.flush();
                        }
                }
        }

        /**
         * 获取可用模板列表
         */
        @GetMapping("/templates")
        @ResponseBody
        public List<TemplateInfo> getTemplates() {
                return posterRenderService.getAllTemplates().stream()
                                .map(t -> new TemplateInfo(t.getTemplateId(), t.getTemplateName(), t.getDescription()))
                                .toList();
        }

        public record TemplateInfo(String id, String name, String description) {
        }
}
