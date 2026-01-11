package yuan.demo.template;

import yuan.demo.dto.ExifInfo;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * 海报模板抽象基类
 * 
 * 提供所有模板共用的工具方法，包括：
 * - 创建高质量 Graphics2D 对象
 * - 绘制圆角图片
 * - 文字居中定位
 * - 渲染参数文字
 * 
 * 子类只需关注具体的布局逻辑，无需重复实现这些通用功能。
 */
public abstract class AbstractPosterTemplate implements PosterTemplate {

    // ========================================
    // 常用颜色常量
    // ========================================

    /** 主文字颜色（深灰） */
    protected static final Color COLOR_TEXT_PRIMARY = new Color(26, 26, 26);

    /** 次要文字颜色（中灰） */
    protected static final Color COLOR_TEXT_SECONDARY = new Color(136, 136, 136);

    /** 白色背景 */
    protected static final Color COLOR_WHITE = Color.WHITE;

    // ========================================
    // Graphics2D 工具方法
    // ========================================

    /**
     * 创建高质量 Graphics2D 对象
     * 
     * 开启抗锯齿和高质量渲染，确保文字和图片边缘平滑。
     * 
     * Graphics2D 渲染质量关键设置：
     * - KEY_ANTIALIASING: 抗锯齿，使边缘更平滑
     * - KEY_TEXT_ANTIALIASING: 文字抗锯齿
     * - KEY_RENDERING: 渲染质量优先
     * - KEY_INTERPOLATION: 图片缩放时使用双三次插值
     *
     * @param image 目标图片
     * @return 配置好的 Graphics2D 对象
     */
    protected Graphics2D createHighQualityGraphics(BufferedImage image) {
        Graphics2D g2d = image.createGraphics();

        // 开启抗锯齿 - 使图形边缘更平滑
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // 开启文字抗锯齿 - 使文字更清晰
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        // 高质量渲染模式
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        // 高质量图片缩放（双三次插值）
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        return g2d;
    }

    /**
     * 绘制圆角图片
     * 
     * 将原始图片裁剪为圆角矩形，用于更现代的视觉效果。
     * 
     * 实现原理：
     * 1. 创建一个新的透明画布
     * 2. 先填充一个圆角矩形区域
     * 3. 使用 SRC_IN 合成模式，只保留与圆角矩形重叠的图片部分
     *
     * @param image        原始图片
     * @param cornerRadius 圆角半径（像素）
     * @return 带圆角的图片
     */
    protected BufferedImage createRoundedImage(BufferedImage image, int cornerRadius) {
        int width = image.getWidth();
        int height = image.getHeight();

        // 创建支持透明度的新图片
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = createHighQualityGraphics(output);

        // 绘制圆角矩形蒙版
        g2d.setColor(Color.WHITE);
        g2d.fill(new RoundRectangle2D.Double(0, 0, width, height, cornerRadius, cornerRadius));

        // 使用 SRC_IN 合成模式：只绘制与蒙版重叠的部分
        g2d.setComposite(AlphaComposite.SrcIn);
        g2d.drawImage(image, 0, 0, null);

        g2d.dispose();
        return output;
    }

    /**
     * 在指定区域内绘制居中图片
     * 
     * 计算图片在目标区域内的居中位置，并绘制。
     * 图片会保持原始尺寸，不会拉伸或缩放。
     *
     * @param g2d        Graphics2D 对象
     * @param image      要绘制的图片
     * @param areaX      目标区域左上角 X 坐标
     * @param areaY      目标区域左上角 Y 坐标
     * @param areaWidth  目标区域宽度
     * @param areaHeight 目标区域高度
     */
    protected void drawCenteredImage(Graphics2D g2d, BufferedImage image,
            int areaX, int areaY, int areaWidth, int areaHeight) {
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        // 计算居中位置
        int x = areaX + (areaWidth - imgWidth) / 2;
        int y = areaY + (areaHeight - imgHeight) / 2;

        g2d.drawImage(image, x, y, null);
    }

    // ========================================
    // 文字渲染工具方法
    // ========================================

    /**
     * 获取文字的宽度（像素）
     * 
     * 用于计算文字居中位置。
     * 
     * FontMetrics 详解：
     * - stringWidth(): 获取字符串的像素宽度
     * - getHeight(): 获取字体行高
     * - getAscent(): 基线到顶部的距离
     * - getDescent(): 基线到底部的距离
     *
     * @param g2d  Graphics2D 对象（需要先设置好字体）
     * @param text 要测量的文字
     * @return 文字宽度（像素）
     */
    protected int getTextWidth(Graphics2D g2d, String text) {
        FontMetrics metrics = g2d.getFontMetrics();
        return metrics.stringWidth(text);
    }

    /**
     * 获取字体行高
     *
     * @param g2d Graphics2D 对象
     * @return 行高（像素）
     */
    protected int getTextHeight(Graphics2D g2d) {
        FontMetrics metrics = g2d.getFontMetrics();
        return metrics.getHeight();
    }

    /**
     * 在指定 Y 坐标绘制水平居中的文字
     * 
     * Graphics2D 文字坐标系说明：
     * - drawString(text, x, y) 中的 y 是文字的【基线】位置，不是顶部！
     * - 基线（Baseline）：大多数字母底部的参考线
     * - 如果想让文字顶部对齐某个位置，需要加上 ascent 值
     * - 如果想让文字垂直居中，需要考虑 ascent 和 descent
     *
     * @param g2d         Graphics2D 对象
     * @param text        要绘制的文字
     * @param canvasWidth 画布宽度（用于计算居中位置）
     * @param y           文字基线的 Y 坐标
     */
    protected void drawCenteredText(Graphics2D g2d, String text, int canvasWidth, int y) {
        int textWidth = getTextWidth(g2d, text);
        int x = (canvasWidth - textWidth) / 2;
        g2d.drawString(text, x, y);
    }

    /**
     * 格式化并绘制摄影参数行
     * 
     * 将多个参数格式化为一行，用分隔符连接：
     * "50mm f/2.8 1/250s ISO 100"
     *
     * @param g2d         Graphics2D 对象
     * @param exifInfo    EXIF 信息
     * @param canvasWidth 画布宽度
     * @param y           文字基线 Y 坐标
     * @param font        使用的字体
     * @param color       文字颜色
     */
    protected void drawParamsLine(Graphics2D g2d, ExifInfo exifInfo,
            int canvasWidth, int y, Font font, Color color) {
        g2d.setFont(font);
        g2d.setColor(color);

        // 构建参数字符串，使用双空格分隔
        String params = String.format("%s  %s  %s  ISO %s",
                formatFocalLength(exifInfo.getFocalLength()),
                exifInfo.getAperture(),
                formatShutterSpeed(exifInfo.getShutterSpeed()),
                formatIso(exifInfo.getIso()));

        drawCenteredText(g2d, params, canvasWidth, y);
    }

    // ========================================
    // 参数格式化工具方法
    // ========================================

    /**
     * 格式化焦距显示
     * 
     * 输入: "50 mm" 或 "50.0 mm"
     * 输出: "50mm"
     */
    protected String formatFocalLength(String focalLength) {
        if (focalLength == null || focalLength.equals("未知")) {
            return "?mm";
        }
        // 移除空格，简化显示
        return focalLength.replace(" ", "").replace(".0", "");
    }

    /**
     * 格式化快门速度显示
     * 
     * 输入: "1/250 sec" 或 "0.01 sec"
     * 输出: "1/250s" 或 "1/100s"
     */
    protected String formatShutterSpeed(String shutterSpeed) {
        if (shutterSpeed == null || shutterSpeed.equals("未知")) {
            return "?s";
        }
        // 移除 "sec"，简化为 "s"
        return shutterSpeed.replace(" sec", "s").replace("sec", "s");
    }

    /**
     * 格式化 ISO 显示
     * 
     * 输入: "100" 或 "ISO 100"
     * 输出: "100"
     */
    protected String formatIso(String iso) {
        if (iso == null || iso.equals("未知")) {
            return "?";
        }
        return iso.replace("ISO ", "").replace("ISO", "");
    }

    /**
     * 获取相机型号（简化版）
     * 
     * 输入: "NIKON CORPORATION NIKON Z 8"
     * 输出: "NIKON Z 8"
     */
    protected String formatCameraModel(ExifInfo exifInfo) {
        String model = exifInfo.getCameraModel();
        if (model == null || model.equals("未知")) {
            return "Unknown Camera";
        }
        // 如果型号中已包含品牌名，直接返回
        return model;
    }
}
