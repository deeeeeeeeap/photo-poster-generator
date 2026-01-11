package yuan.demo.template;

import yuan.demo.dto.ExifInfo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * 经典白底模板
 * 
 * 设计风格：
 * - 纯白背景，简洁大气
 * - 照片居中显示（可选圆角）
 * - 下方显示相机型号和摄影参数
 */
public class ClassicTemplate extends AbstractPosterTemplate {

    // ========================================
    // 模板配置常量（比例值，相对于照片短边）
    // ========================================

    /** 画布内边距比例（增大到 5%） */
    private static final double PADDING_RATIO = 0.05;

    /** 照片与文字区域的间距比例 */
    private static final double PHOTO_TEXT_GAP_RATIO = 0.025;

    /** 相机型号与镜头型号的间距比例 */
    private static final double MODEL_GAP_RATIO = 0.01;

    /** 分隔线与文字的间距比例 */
    private static final double SEPARATOR_GAP_RATIO = 0.018;

    /** 分隔线长度（相对于画布宽度的比例） */
    private static final double SEPARATOR_WIDTH_RATIO = 0.4;

    /** 照片圆角半径比例 */
    private static final double CORNER_RADIUS_RATIO = 0.008;

    /** 相机型号字体大小比例（增大） */
    private static final double FONT_CAMERA_RATIO = 0.035;

    /** 镜头型号字体大小比例 */
    private static final double FONT_LENS_RATIO = 0.02;

    /** 参数字体大小比例（增大） */
    private static final double FONT_PARAMS_RATIO = 0.025;

    // ========================================
    // 接口实现
    // ========================================

    @Override
    public String getTemplateId() {
        return "classic";
    }

    @Override
    public String getTemplateName() {
        return "经典白底";
    }

    @Override
    public String getDescription() {
        return "简洁大气的白色背景";
    }

    @Override
    public BufferedImage render(BufferedImage originalImage, ExifInfo exifInfo) throws IOException {

        int photoWidth = originalImage.getWidth();
        int photoHeight = originalImage.getHeight();
        int baseSize = Math.min(photoWidth, photoHeight);

        // 根据比例计算各个尺寸
        int padding = (int) (baseSize * PADDING_RATIO);
        int photoTextGap = (int) (baseSize * PHOTO_TEXT_GAP_RATIO);
        int modelGap = (int) (baseSize * MODEL_GAP_RATIO);
        int separatorGap = (int) (baseSize * SEPARATOR_GAP_RATIO);
        int cornerRadius = (int) (baseSize * CORNER_RADIUS_RATIO);

        // 字体大小
        int fontCameraSize = Math.max(28, (int) (baseSize * FONT_CAMERA_RATIO));
        int fontLensSize = Math.max(18, (int) (baseSize * FONT_LENS_RATIO));
        int fontParamsSize = Math.max(22, (int) (baseSize * FONT_PARAMS_RATIO));

        Font fontCamera = new Font("SansSerif", Font.BOLD, fontCameraSize);
        Font fontLens = new Font("SansSerif", Font.PLAIN, fontLensSize);
        Font fontParams = new Font("SansSerif", Font.PLAIN, fontParamsSize);

        // 画布宽度 = 照片宽度 + 左右内边距
        int canvasWidth = photoWidth + padding * 2;

        // 计算文字区域高度
        BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D tempG2d = createHighQualityGraphics(tempImage);

        tempG2d.setFont(fontCamera);
        int cameraHeight = tempG2d.getFontMetrics().getHeight();

        tempG2d.setFont(fontLens);
        int lensHeight = tempG2d.getFontMetrics().getHeight();

        tempG2d.setFont(fontParams);
        int paramsHeight = tempG2d.getFontMetrics().getHeight();

        tempG2d.dispose();

        // 文字区域总高度
        int textAreaHeight = photoTextGap + cameraHeight + modelGap + lensHeight
                + separatorGap + 1 + separatorGap + paramsHeight;

        // 画布高度
        int canvasHeight = padding + photoHeight + textAreaHeight + padding;

        // 创建画布
        BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = createHighQualityGraphics(canvas);

        // 填充白色背景
        g2d.setColor(COLOR_WHITE);
        g2d.fillRect(0, 0, canvasWidth, canvasHeight);

        // 绘制照片
        BufferedImage photoToDraw = originalImage;
        if (cornerRadius > 0) {
            photoToDraw = createRoundedImage(originalImage, cornerRadius);
        }

        int photoX = padding;
        int photoY = padding;
        g2d.drawImage(photoToDraw, photoX, photoY, null);

        // 绘制相机型号
        int currentY = padding + photoHeight + photoTextGap;

        g2d.setFont(fontCamera);
        g2d.setColor(COLOR_TEXT_PRIMARY);
        String cameraModel = formatCameraModel(exifInfo);
        FontMetrics cameraMetrics = g2d.getFontMetrics();
        int cameraBaseline = currentY + cameraMetrics.getAscent();
        drawCenteredText(g2d, cameraModel, canvasWidth, cameraBaseline);
        currentY += cameraMetrics.getHeight() + modelGap;

        // 绘制镜头型号
        g2d.setFont(fontLens);
        g2d.setColor(COLOR_TEXT_SECONDARY);
        String lensModel = exifInfo.getLensModel();
        if (lensModel == null || lensModel.equals("未知")) {
            lensModel = "";
        }
        if (!lensModel.isEmpty()) {
            FontMetrics lensMetrics = g2d.getFontMetrics();
            int lensBaseline = currentY + lensMetrics.getAscent();
            drawCenteredText(g2d, lensModel, canvasWidth, lensBaseline);
            currentY += lensMetrics.getHeight();
        }
        currentY += separatorGap;

        // 绘制分隔线
        g2d.setColor(new Color(220, 220, 220));
        int separatorWidth = (int) (canvasWidth * SEPARATOR_WIDTH_RATIO);
        int separatorX = (canvasWidth - separatorWidth) / 2;
        g2d.fillRect(separatorX, currentY, separatorWidth, 1);
        currentY += 1 + separatorGap;

        // 绘制参数行
        g2d.setFont(fontParams);
        g2d.setColor(COLOR_TEXT_PRIMARY);
        FontMetrics paramsMetrics = g2d.getFontMetrics();
        int paramsBaseline = currentY + paramsMetrics.getAscent();
        drawParamsLine(g2d, exifInfo, canvasWidth, paramsBaseline, fontParams, COLOR_TEXT_PRIMARY);

        g2d.dispose();
        return canvas;
    }
}
