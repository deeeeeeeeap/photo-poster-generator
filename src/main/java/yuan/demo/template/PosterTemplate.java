package yuan.demo.template;

import yuan.demo.dto.ExifInfo;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * 海报模板接口
 * 
 * 所有海报模板都需要实现此接口。
 * 通过定义统一的接口，可以方便地扩展新的模板样式，
 * 同时保持渲染服务层代码的稳定性。
 * 
 * 设计模式：策略模式（Strategy Pattern）
 * - 每个模板是一个具体策略
 * - 渲染服务作为上下文，根据用户选择调用不同策略
 */
public interface PosterTemplate {

    /**
     * 获取模板的唯一标识符
     * 
     * 用于前端选择模板时的参数传递，以及模板的持久化存储。
     * 例如: "classic", "blur-background", "minimalist"
     *
     * @return 模板 ID（英文小写，使用连字符分隔）
     */
    String getTemplateId();

    /**
     * 获取模板的显示名称
     * 
     * 用于前端 UI 展示，应该是用户友好的中文名称。
     * 例如: "经典白底", "毛玻璃背景", "极简风格"
     *
     * @return 模板显示名称
     */
    String getTemplateName();

    /**
     * 获取模板的描述信息
     * 
     * 简短描述模板的特点和适用场景。
     *
     * @return 模板描述
     */
    String getDescription();

    /**
     * 渲染海报
     * 
     * 这是模板的核心方法。接收原始图片和 EXIF 信息，
     * 返回渲染完成的海报图片。
     * 
     * 实现要点：
     * 1. 必须保持原图画质，不进行压缩
     * 2. 合理计算画布尺寸，确保水印区域足够
     * 3. 使用抗锯齿渲染，保证文字清晰
     *
     * @param originalImage 用户上传的原始图片
     * @param exifInfo      从图片中提取的 EXIF 信息
     * @return 渲染完成的海报图片
     * @throws IOException 当图像处理失败时抛出
     */
    BufferedImage render(BufferedImage originalImage, ExifInfo exifInfo) throws IOException;
}
