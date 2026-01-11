package yuan.demo.template;

import com.jhlabs.image.GaussianFilter;
import yuan.demo.dto.ExifInfo;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 毛玻璃背景模板（专业版 v3 - 性能优化）
 * 
 * 性能优化：
 * 1. 减少图像缓冲区创建次数
 * 2. 降低背景处理尺寸（更快的模糊）
 * 3. 复用 Graphics2D 对象
 * 4. 预计算常用值
 */
public class BlurBackgroundTemplate extends AbstractPosterTemplate {

    // ========================================
    // 模板配置常量
    // ========================================

    private static final double BORDER_RATIO = 0.08;
    private static final double CORNER_RADIUS_RATIO = 0.03;
    private static final double WATERMARK_HEIGHT_RATIO = 0.18;
    private static final double FONT_BRAND_RATIO = 0.045;
    private static final double FONT_LENS_RATIO = 0.02;
    private static final double FONT_PARAMS_RATIO = 0.028;
    private static final double BLUR_RADIUS_RATIO = 0.05;

    /** 背景缩放比例（降低到 0.25 提升性能） */
    private static final double BG_SCALE_FACTOR = 0.25;

    private static final double LETTER_SPACING_RATIO = 0.12;
    private static final double LINE_SPACING_RATIO = 0.015;
    private static final double SHADOW_EXPAND_RATIO = 0.012;
    private static final double SHADOW_BLUR_RATIO = 0.015;
    private static final double SHADOW_OFFSET_RATIO = 0.006;

    /** 复用高斯滤镜实例 */
    private final GaussianFilter bgGaussianFilter = new GaussianFilter();
    private final GaussianFilter shadowGaussianFilter = new GaussianFilter();

    @Override
    public String getTemplateId() {
        return "blur-background";
    }

    @Override
    public String getTemplateName() {
        return "毛玻璃背景";
    }

    @Override
    public String getDescription() {
        return "照片周围显示模糊背景边框，现代感强";
    }

    @Override
    public BufferedImage render(BufferedImage originalImage, ExifInfo exifInfo) throws IOException {
        int photoWidth = originalImage.getWidth();
        int photoHeight = originalImage.getHeight();
        int baseSize = Math.min(photoWidth, photoHeight);

        // 预计算所有尺寸（避免重复计算）
        int borderWidth = (int) (baseSize * BORDER_RATIO);
        int cornerRadius = (int) (baseSize * CORNER_RADIUS_RATIO);
        int watermarkHeight = (int) (baseSize * WATERMARK_HEIGHT_RATIO);
        int lineSpacing = (int) (baseSize * LINE_SPACING_RATIO);
        int shadowExpand = (int) (baseSize * SHADOW_EXPAND_RATIO);
        int shadowBlurRadius = Math.max(8, (int) (baseSize * SHADOW_BLUR_RATIO));
        int shadowOffset = (int) (baseSize * SHADOW_OFFSET_RATIO);

        int fontBrandSize = Math.max(28, (int) (baseSize * FONT_BRAND_RATIO));
        int fontLensSize = Math.max(18, (int) (baseSize * FONT_LENS_RATIO));
        int fontParamsSize = Math.max(22, (int) (baseSize * FONT_PARAMS_RATIO));

        int canvasWidth = photoWidth + borderWidth * 2;
        int canvasHeight = photoHeight + borderWidth * 2 + watermarkHeight;
        int photoX = borderWidth;
        int photoY = borderWidth;

        // 创建画布（使用 RGB 而非 ARGB，更快）
        BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = createHighQualityGraphics(canvas);

        // ========== 第一层：模糊背景 ==========
        drawBlurredBackground(g2d, originalImage, canvasWidth, canvasHeight);

        // ========== 第二层：底部渐变 ==========
        int gradientStartY = photoY + photoHeight - borderWidth;
        g2d.setPaint(new GradientPaint(0, gradientStartY, new Color(0, 0, 0, 0),
                0, canvasHeight, new Color(0, 0, 0, 100)));
        g2d.fillRect(0, gradientStartY, canvasWidth, canvasHeight - gradientStartY);

        // ========== 第三层：柔焦投影 ==========
        BufferedImage shadow = createSoftDropShadow(photoWidth, photoHeight, cornerRadius, shadowExpand,
                shadowBlurRadius);
        g2d.drawImage(shadow, photoX - shadowExpand, photoY - shadowExpand + shadowOffset, null);

        // ========== 第四层：圆角照片 ==========
        BufferedImage roundedPhoto = createRoundedPhoto(originalImage, cornerRadius);
        g2d.drawImage(roundedPhoto, photoX, photoY, null);

        // ========== 第五层：文字 ==========
        drawTextLayer(g2d, exifInfo, canvas, canvasWidth, canvasHeight,
                photoY + photoHeight, borderWidth, lineSpacing, watermarkHeight,
                fontBrandSize, fontLensSize, fontParamsSize);

        g2d.dispose();
        return canvas;
    }

    /**
     * 绘制模糊背景（优化版）
     */
    private void drawBlurredBackground(Graphics2D g2d, BufferedImage source, int targetWidth, int targetHeight) {
        // 计算缩小尺寸
        int processWidth = Math.max(100, (int) (targetWidth * BG_SCALE_FACTOR));
        int processHeight = Math.max(100, (int) (targetHeight * BG_SCALE_FACTOR));

        // 直接缩放到小尺寸
        BufferedImage small = new BufferedImage(processWidth, processHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // 计算居中裁剪
        double sourceRatio = (double) source.getWidth() / source.getHeight();
        double targetRatio = (double) targetWidth / targetHeight;
        int srcX = 0, srcY = 0, srcW = source.getWidth(), srcH = source.getHeight();

        if (sourceRatio > targetRatio) {
            srcW = (int) (source.getHeight() * targetRatio);
            srcX = (source.getWidth() - srcW) / 2;
        } else {
            srcH = (int) (source.getWidth() / targetRatio);
            srcY = (source.getHeight() - srcH) / 2;
        }

        g.drawImage(source, 0, 0, processWidth, processHeight, srcX, srcY, srcX + srcW, srcY + srcH, null);
        g.dispose();

        // 应用高斯模糊
        int shortSide = Math.min(processWidth, processHeight);
        float blurRadius = Math.max(10, Math.min((float) (shortSide * BLUR_RADIUS_RATIO), 60));
        bgGaussianFilter.setRadius(blurRadius);
        BufferedImage blurred = bgGaussianFilter.filter(small, null);

        // 放大绘制到画布
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(blurred, 0, 0, targetWidth, targetHeight, null);

        // 暗色遮罩
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillRect(0, 0, targetWidth, targetHeight);
    }

    /**
     * 创建圆角照片（优化版）
     */
    private BufferedImage createRoundedPhoto(BufferedImage source, int cornerRadius) {
        int w = source.getWidth();
        int h = source.getHeight();

        BufferedImage rounded = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rounded.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setClip(new RoundRectangle2D.Double(0, 0, w, h, cornerRadius * 2, cornerRadius * 2));
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();

        return rounded;
    }

    /**
     * 创建柔焦投影（优化版）
     */
    private BufferedImage createSoftDropShadow(int photoWidth, int photoHeight,
            int cornerRadius, int expand, int blurRadius) {
        int shadowWidth = photoWidth + expand * 2;
        int shadowHeight = photoHeight + expand * 2;

        BufferedImage shadowBase = new BufferedImage(shadowWidth, shadowHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = shadowBase.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(0, 0, 0, 140));
        g2d.fill(new RoundRectangle2D.Double(expand, expand, photoWidth, photoHeight, cornerRadius * 2,
                cornerRadius * 2));
        g2d.dispose();

        shadowGaussianFilter.setRadius(Math.min(blurRadius, 50));
        return shadowGaussianFilter.filter(shadowBase, null);
    }

    /**
     * 绘制文字层（优化版）
     */
    private void drawTextLayer(Graphics2D g2d, ExifInfo exifInfo, BufferedImage canvas,
            int canvasWidth, int canvasHeight, int photoBottom,
            int borderWidth, int lineSpacing, int watermarkHeight,
            int fontBrandSize, int fontLensSize, int fontParamsSize) {

        // 智能对比度（简化采样）
        double brightness = sampleBrightness(canvas, canvasHeight - watermarkHeight, watermarkHeight);
        Color textColor = brightness < 0.45 ? Color.WHITE : Color.BLACK;
        Color shadowColor = brightness < 0.45 ? new Color(0, 0, 0, 120) : new Color(255, 255, 255, 100);

        int textY = photoBottom + borderWidth / 2 + lineSpacing;

        // 相机型号
        Font fontBrand = new Font("SansSerif", Font.BOLD, fontBrandSize);
        textY = drawTextLine(g2d, formatCameraModel(exifInfo), canvasWidth, textY, fontBrand, textColor, shadowColor);
        textY += lineSpacing;

        // 镜头型号
        String lensModel = exifInfo.getLensModel();
        if (lensModel != null && !lensModel.equals("未知") && !lensModel.isEmpty()) {
            Font fontLens = new Font("SansSerif", Font.PLAIN, fontLensSize);
            textY = drawTextLine(g2d, lensModel, canvasWidth, textY, fontLens, textColor, shadowColor);
            textY += lineSpacing * 2;
        }

        // 参数行
        Font fontParams = new Font("SansSerif", Font.PLAIN, fontParamsSize);
        String paramsText = String.format("%s   %s   %s   ISO %s",
                formatFocalLength(exifInfo.getFocalLength()),
                exifInfo.getAperture() != null ? exifInfo.getAperture() : "f/?",
                formatShutterSpeed(exifInfo.getShutterSpeed()),
                formatIso(exifInfo.getIso()));
        drawTextLine(g2d, paramsText, canvasWidth, textY, fontParams, textColor, shadowColor);
    }

    /**
     * 绘制单行文字（带投影和字符间距）
     */
    private int drawTextLine(Graphics2D g2d, String text, int canvasWidth, int y,
            Font font, Color textColor, Color shadowColor) {
        Map<TextAttribute, Object> attrs = new HashMap<>();
        attrs.put(TextAttribute.FONT, font);
        attrs.put(TextAttribute.TRACKING, (float) LETTER_SPACING_RATIO);
        Font spacedFont = font.deriveFont(attrs);

        g2d.setFont(spacedFont);
        FontMetrics fm = g2d.getFontMetrics();
        int x = (canvasWidth - fm.stringWidth(text)) / 2;
        int baseline = y + fm.getAscent();

        // 投影
        g2d.setColor(shadowColor);
        g2d.drawString(text, x + 2, baseline + 2);

        // 主文字
        g2d.setColor(textColor);
        g2d.drawString(text, x, baseline);

        return baseline + fm.getDescent();
    }

    /**
     * 快速采样亮度
     */
    private double sampleBrightness(BufferedImage image, int startY, int height) {
        int w = image.getWidth();
        int h = Math.min(height, image.getHeight() - startY);
        if (h <= 0)
            return 0.5;

        long total = 0;
        int count = 0;
        int step = Math.max(w / 20, 10);

        for (int y = startY; y < startY + h; y += step) {
            for (int x = 0; x < w; x += step) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                total += (int) (0.299 * r + 0.587 * g + 0.114 * b);
                count++;
            }
        }

        return count > 0 ? (total / (double) count) / 255.0 : 0.5;
    }
}
