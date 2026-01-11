package yuan.demo.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yuan.demo.dto.ExifInfo;
import yuan.demo.template.BlurBackgroundTemplate;
import yuan.demo.template.ClassicTemplate;
import yuan.demo.template.PosterTemplate;

import jakarta.annotation.PostConstruct;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

/**
 * 海报渲染服务
 * 
 * 负责管理所有海报模板，并提供图像渲染和输出功能。
 * 
 * 主要功能：
 * 1. 注册和管理多个海报模板
 * 2. 根据模板 ID 调用对应模板进行渲染
 * 3. 输出高质量 PNG/JPEG 图像
 * 4. 处理 EXIF Orientation 标签，自动旋转图片
 */
@Slf4j
@Service
public class PosterRenderService {

    /** 模板注册表：模板ID -> 模板实例 */
    private final Map<String, PosterTemplate> templateRegistry = new LinkedHashMap<>();

    /** 默认模板 ID */
    private static final String DEFAULT_TEMPLATE_ID = "classic";

    /**
     * 初始化模板注册表
     */
    @PostConstruct
    public void init() {
        registerTemplate(new ClassicTemplate());
        registerTemplate(new BlurBackgroundTemplate());
        log.info("海报模板注册完成，共 {} 个模板: {}",
                templateRegistry.size(),
                templateRegistry.keySet());
    }

    public void registerTemplate(PosterTemplate template) {
        templateRegistry.put(template.getTemplateId(), template);
    }

    public List<PosterTemplate> getAllTemplates() {
        return new ArrayList<>(templateRegistry.values());
    }

    public PosterTemplate getTemplate(String templateId) {
        PosterTemplate template = templateRegistry.get(templateId);
        if (template == null) {
            log.warn("模板 {} 不存在，使用默认模板 {}", templateId, DEFAULT_TEMPLATE_ID);
            return templateRegistry.get(DEFAULT_TEMPLATE_ID);
        }
        return template;
    }

    public BufferedImage renderPoster(String templateId, BufferedImage originalImage, ExifInfo exifInfo)
            throws IOException {

        PosterTemplate template = getTemplate(templateId);

        log.info("使用模板 [{}] 渲染海报，原图尺寸: {}x{}",
                template.getTemplateName(),
                originalImage.getWidth(),
                originalImage.getHeight());

        long startTime = System.currentTimeMillis();

        BufferedImage poster = template.render(originalImage, exifInfo);

        long duration = System.currentTimeMillis() - startTime;
        log.info("海报渲染完成，耗时 {}ms，输出尺寸: {}x{}",
                duration,
                poster.getWidth(),
                poster.getHeight());

        return poster;
    }

    /**
     * 从 Base64 数据加载图片，并自动根据 EXIF Orientation 旋转
     */
    public BufferedImage loadImageFromBase64(String base64Data) throws IOException {
        // 移除 Data URL 前缀
        String pureBase64 = base64Data;
        if (base64Data.contains(",")) {
            pureBase64 = base64Data.substring(base64Data.indexOf(",") + 1);
        }

        byte[] imageBytes = Base64.getDecoder().decode(pureBase64);

        // 读取 EXIF Orientation
        int orientation = 1;
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            Metadata metadata = ImageMetadataReader.readMetadata(is);
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                log.debug("检测到 EXIF Orientation: {}", orientation);
            }
        } catch (Exception e) {
            log.debug("无法读取 EXIF Orientation: {}", e.getMessage());
        }

        // 读取图片
        BufferedImage image;
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            image = ImageIO.read(is);
        }

        // 根据 Orientation 旋转图片
        return applyExifOrientation(image, orientation);
    }

    /**
     * 根据 EXIF Orientation 标签旋转图片
     * 
     * Orientation 值含义：
     * 1 = 正常
     * 2 = 水平翻转
     * 3 = 旋转 180°
     * 4 = 垂直翻转
     * 5 = 顺时针 90° + 水平翻转
     * 6 = 顺时针 90°
     * 7 = 逆时针 90° + 水平翻转
     * 8 = 逆时针 90°
     */
    private BufferedImage applyExifOrientation(BufferedImage image, int orientation) {
        if (orientation == 1) {
            return image; // 正常方向，无需处理
        }

        int width = image.getWidth();
        int height = image.getHeight();

        AffineTransform transform = new AffineTransform();
        int newWidth = width;
        int newHeight = height;

        switch (orientation) {
            case 2: // 水平翻转
                transform.scale(-1, 1);
                transform.translate(-width, 0);
                break;
            case 3: // 旋转 180°
                transform.rotate(Math.PI, width / 2.0, height / 2.0);
                break;
            case 4: // 垂直翻转
                transform.scale(1, -1);
                transform.translate(0, -height);
                break;
            case 5: // 顺时针 90° + 水平翻转
                newWidth = height;
                newHeight = width;
                transform.rotate(Math.PI / 2);
                transform.scale(1, -1);
                break;
            case 6: // 顺时针 90°
                newWidth = height;
                newHeight = width;
                transform.rotate(Math.PI / 2);
                transform.translate(0, -height);
                break;
            case 7: // 逆时针 90° + 水平翻转
                newWidth = height;
                newHeight = width;
                transform.rotate(-Math.PI / 2);
                transform.scale(1, -1);
                transform.translate(-width, 0);
                break;
            case 8: // 逆时针 90°
                newWidth = height;
                newHeight = width;
                transform.rotate(-Math.PI / 2);
                transform.translate(-width, 0);
                break;
            default:
                return image;
        }

        log.info("应用 EXIF Orientation {} 旋转图片: {}x{} -> {}x{}",
                orientation, width, height, newWidth, newHeight);

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = rotated.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setTransform(transform);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        return rotated;
    }

    public BufferedImage loadImage(InputStream inputStream) throws IOException {
        return ImageIO.read(inputStream);
    }

    public byte[] exportAsPng(BufferedImage image) throws IOException {
        // 预分配缓冲区（估算大小：宽*高*3 bytes，通常 PNG 压缩后会更小）
        int estimatedSize = Math.max(1024 * 1024, image.getWidth() * image.getHeight() / 2);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(estimatedSize);
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    public byte[] exportAsJpeg(BufferedImage image, float quality) throws IOException {
        // 预分配缓冲区
        int estimatedSize = Math.max(512 * 1024, image.getWidth() * image.getHeight() / 4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(estimatedSize);

        // 如果图像有透明通道，先转换为 RGB（JPEG 不支持透明）
        BufferedImage rgbImage = image;
        if (image.getType() == BufferedImage.TYPE_INT_ARGB ||
                image.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgbImage.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.drawImage(image, 0, 0, null);
            g.dispose();
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("没有可用的 JPEG 编码器");
        }
        ImageWriter writer = writers.next();

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgbImage, null, null), param);
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }
}
