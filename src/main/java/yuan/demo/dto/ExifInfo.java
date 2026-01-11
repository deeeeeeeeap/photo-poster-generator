package yuan.demo.dto;

import lombok.Builder;
import lombok.Data;

/**
 * EXIF 信息数据传输对象
 * 
 * 用于封装从图片中提取的摄影参数信息，
 * 包括快门速度、感光度（ISO）、光圈值、焦距、镜头型号、曝光补偿等关键拍摄数据。
 * 
 * 这些参数将被用于生成摄影参数海报，展示拍摄时的技术细节。
 */
@Data
@Builder
public class ExifInfo {

    // ========================================
    // 相机与镜头信息
    // ========================================

    /**
     * 相机制造商
     * 例如: "NIKON CORPORATION" 或 "Canon"
     */
    private String cameraMake;

    /**
     * 相机型号
     * 例如: "NIKON Z 8" 或 "Canon EOS R5"
     */
    private String cameraModel;

    /**
     * 镜头型号
     * 例如: "NIKKOR Z 24-70mm f/2.8 S" 或 "RF24-70mm F2.8 L IS USM"
     * 
     * 注意：镜头信息存储在 EXIF 的不同位置，可能在 ExifSubIFD 或 Makernotes 目录中
     */
    private String lensModel;

    // ========================================
    // 核心摄影参数（曝光三要素 + 焦距）
    // ========================================

    /**
     * 快门速度（曝光时间）
     * 例如: "1/250 sec" 或 "1/60 sec"
     * 
     * 快门速度决定了传感器接收光线的时间长度，
     * 较快的快门可以凝固动作，较慢的快门可以产生运动模糊效果。
     */
    private String shutterSpeed;

    /**
     * 光圈值（F-Number）
     * 例如: "f/2.8" 或 "f/5.6"
     * 
     * 光圈控制进光量和景深，
     * 较大光圈（小F值）产生更浅的景深和更强的背景虚化效果。
     */
    private String aperture;

    /**
     * 感光度（ISO）
     * 例如: "100" 或 "3200"
     * 
     * ISO 决定传感器对光线的敏感度，
     * 较低的 ISO 画质更好，较高的 ISO 适用于暗光环境但可能产生噪点。
     */
    private String iso;

    /**
     * 焦距
     * 例如: "50 mm" 或 "24 mm"
     * 
     * 焦距决定了视角范围和透视效果，
     * 广角（小焦距）适合风景，长焦（大焦距）适合人像和远距离拍摄。
     */
    private String focalLength;

    // ========================================
    // 扩展摄影参数
    // ========================================

    /**
     * 曝光补偿（Exposure Bias / EV）
     * 例如: "+1 EV" 或 "-0.7 EV" 或 "0 EV"
     * 
     * 曝光补偿允许摄影师手动调整相机的自动测光结果，
     * 正值使画面更亮，负值使画面更暗。
     */
    private String exposureBias;

    /**
     * 测光模式
     * 例如: "Multi-segment" 或 "Center-weighted average" 或 "Spot"
     * 
     * 测光模式决定相机如何测量场景的亮度来计算曝光。
     */
    private String meteringMode;

    /**
     * 白平衡模式
     * 例如: "Auto" 或 "Daylight" 或 "Cloudy"
     * 
     * 白平衡用于校正不同光源下的色温偏差。
     */
    private String whiteBalance;

    /**
     * 闪光灯状态
     * 例如: "Flash did not fire" 或 "Flash fired"
     */
    private String flash;

    // ========================================
    // 时间与文件信息
    // ========================================

    /**
     * 拍摄日期时间
     * 例如: "2026:01:11 18:30:00"
     */
    private String dateTime;

    /**
     * 原始文件名
     * 例如: "DSC_0073.JPG"
     */
    private String originalFileName;

    /**
     * Base64 编码的缩略图数据
     * 用于在海报页面中显示上传的图片预览
     * 
     * 格式: "data:image/jpeg;base64,/9j/4AAQ..."
     */
    private String imageBase64;
}
