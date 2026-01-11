package yuan.demo.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import yuan.demo.dto.ExifInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * EXIF 信息提取服务（增强版）
 * 
 * 该服务使用 metadata-extractor 库从上传的图片文件中提取完整的 EXIF 元数据，
 * 支持提取相机型号、镜头型号、快门、光圈、ISO、焦距、曝光补偿等专业摄影参数。
 * 
 * 主要功能：
 * - 提取 EXIF IFD0 目录中的相机基本信息（制造商、型号）
 * - 提取 EXIF SubIFD 目录中的详细拍摄参数（曝光三要素、焦距等）
 * - 自动搜索镜头信息（可能存在于多个 EXIF 目录中）
 * - 将上传图片转换为 Base64 格式，用于海报展示
 */
@Slf4j
@Service
public class ExifExtractorService {

    /**
     * 从上传的图片文件中提取完整的 EXIF 信息
     * 
     * 该方法会读取图片的所有元数据，并将其解析为 ExifInfo 对象。
     * 同时会将图片转换为 Base64 编码，用于在海报页面中展示。
     *
     * @param file 用户上传的图片文件（MultipartFile 格式）
     * @return ExifInfo 包含完整 EXIF 信息和图片 Base64 数据的对象
     * @throws ImageProcessingException 当图片格式不支持或 EXIF 解析失败时抛出
     * @throws IOException              当读取文件流失败时抛出
     */
    public ExifInfo extractExifInfo(MultipartFile file) throws ImageProcessingException, IOException {
        log.info("开始提取 EXIF 信息，文件名: {}, 大小: {} 字节",
                file.getOriginalFilename(), file.getSize());

        // 从上传文件获取输入流并解析元数据
        try (InputStream inputStream = file.getInputStream()) {
            // 使用 metadata-extractor 读取图片的所有元数据
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);

            // 将图片转换为 Base64 编码，用于在海报中显示
            String imageBase64 = convertToBase64(file);

            // 构建并返回 EXIF 信息对象
            ExifInfo exifInfo = buildExifInfo(metadata, file.getOriginalFilename(), imageBase64);

            log.info("EXIF 信息提取完成 - 相机: {} {}, 镜头: {}, 参数: {} | {} | ISO {} | {}",
                    exifInfo.getCameraMake(),
                    exifInfo.getCameraModel(),
                    exifInfo.getLensModel(),
                    exifInfo.getShutterSpeed(),
                    exifInfo.getAperture(),
                    exifInfo.getIso(),
                    exifInfo.getFocalLength());

            return exifInfo;
        }
    }

    /**
     * 将上传的图片文件转换为 Base64 编码字符串
     * 
     * Base64 编码后的图片数据可以直接嵌入 HTML 的 img 标签中，
     * 格式为 "data:image/jpeg;base64,..." 的 Data URL。
     *
     * @param file 上传的图片文件
     * @return Base64 编码的 Data URL 字符串
     * @throws IOException 当读取文件失败时抛出
     */
    private String convertToBase64(MultipartFile file) throws IOException {
        // 获取文件的 MIME 类型（如 image/jpeg）
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "image/jpeg"; // 默认使用 JPEG
        }

        // 读取文件字节并进行 Base64 编码
        byte[] bytes = file.getBytes();
        String base64Data = Base64.getEncoder().encodeToString(bytes);

        // 返回完整的 Data URL 格式
        return "data:" + contentType + ";base64," + base64Data;
    }

    /**
     * 从元数据对象中构建完整的 ExifInfo 实例
     * 
     * 该方法从 Metadata 对象中提取多个 EXIF 目录的信息：
     * - EXIF IFD0：相机制造商、型号等基本信息
     * - EXIF SubIFD：快门、光圈、ISO、焦距、曝光补偿等详细拍摄参数
     * - 其他目录：镜头信息可能存储在不同位置
     *
     * @param metadata         图片的完整元数据对象
     * @param originalFileName 原始文件名
     * @param imageBase64      Base64 编码的图片数据
     * @return ExifInfo 组装好的完整 EXIF 信息对象
     */
    private ExifInfo buildExifInfo(Metadata metadata, String originalFileName, String imageBase64) {
        // 获取 EXIF IFD0 目录 - 包含相机制造商、型号等基本信息
        ExifIFD0Directory ifd0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

        // 获取 EXIF SubIFD 目录 - 包含快门、光圈、ISO、焦距、曝光补偿等详细拍摄参数
        ExifSubIFDDirectory subIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

        // 使用 Builder 模式构建 ExifInfo 对象，提取所有专业摄影参数
        return ExifInfo.builder()
                // ========== 相机与镜头信息 ==========
                // 提取相机制造商（如 "NIKON CORPORATION"）
                .cameraMake(getTagValue(ifd0Directory, ExifIFD0Directory.TAG_MAKE))
                // 提取相机型号（如 "NIKON Z 8"）
                .cameraModel(getTagValue(ifd0Directory, ExifIFD0Directory.TAG_MODEL))
                // 提取镜头型号（需要从多个可能的位置搜索）
                .lensModel(extractLensModel(metadata, subIFDDirectory))

                // ========== 核心摄影参数（曝光三要素 + 焦距） ==========
                // 提取快门速度/曝光时间（如 "1/250 sec"）
                .shutterSpeed(getTagValue(subIFDDirectory, ExifSubIFDDirectory.TAG_EXPOSURE_TIME))
                // 提取光圈值/F-Number（如 "f/2.8"）
                .aperture(getTagValue(subIFDDirectory, ExifSubIFDDirectory.TAG_FNUMBER))
                // 提取感光度 ISO（如 "100"）
                .iso(getTagValue(subIFDDirectory, ExifSubIFDDirectory.TAG_ISO_EQUIVALENT))
                // 提取焦距（如 "50 mm"）
                .focalLength(getTagValue(subIFDDirectory, ExifSubIFDDirectory.TAG_FOCAL_LENGTH))

                // ========== 扩展摄影参数 ==========
                // 提取曝光补偿（如 "+1 EV" 或 "-0.7 EV"）
                .exposureBias(getTagValue(subIFDDirectory, ExifSubIFDDirectory.TAG_EXPOSURE_BIAS))
                // 提取测光模式（如 "Multi-segment"）
                .meteringMode(getTagValue(subIFDDirectory, ExifSubIFDDirectory.TAG_METERING_MODE))
                // 提取白平衡模式（如 "Auto"）
                .whiteBalance(getTagValue(subIFDDirectory, ExifSubIFDDirectory.TAG_WHITE_BALANCE_MODE))
                // 提取闪光灯状态（如 "Flash did not fire"）
                .flash(getTagValue(subIFDDirectory, ExifSubIFDDirectory.TAG_FLASH))

                // ========== 时间与文件信息 ==========
                // 提取原始拍摄日期时间
                .dateTime(getTagValue(subIFDDirectory, ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL))
                // 设置原始文件名
                .originalFileName(originalFileName)
                // 设置 Base64 编码的图片数据
                .imageBase64(imageBase64)
                .build();
    }

    /**
     * 提取镜头型号信息
     * 
     * 镜头信息在不同相机品牌中存储位置可能不同：
     * - 有些相机将镜头信息存储在 ExifSubIFD 目录的 TAG_LENS_MODEL 标签中
     * - 有些相机将镜头信息存储在厂商专用的 Makernotes 目录中
     * - 早期相机可能使用 TAG_LENS 标签
     * 
     * 该方法会依次尝试从多个可能的位置获取镜头信息。
     *
     * @param metadata        完整的元数据对象
     * @param subIFDDirectory EXIF SubIFD 目录对象
     * @return 镜头型号字符串，如果未找到则返回 "未知"
     */
    private String extractLensModel(Metadata metadata, ExifSubIFDDirectory subIFDDirectory) {
        // 首先尝试从 SubIFD 目录获取镜头型号（TAG_LENS_MODEL = 0xA434）
        String lensModel = getTagValue(subIFDDirectory, ExifSubIFDDirectory.TAG_LENS_MODEL);
        if (!lensModel.equals("未知")) {
            return lensModel;
        }

        // 如果 SubIFD 中没有，尝试从所有目录中搜索可能的镜头标签
        // 不同相机品牌可能使用不同的标签存储镜头信息
        for (Directory directory : metadata.getDirectories()) {
            // 尝试查找名为 "Lens Model" 或 "Lens" 的标签
            if (directory instanceof ExifDirectoryBase) {
                ExifDirectoryBase exifDir = (ExifDirectoryBase) directory;

                // 尝试 TAG_LENS（0xFDEA）- 一些相机使用这个标签
                String lens = getTagValue(exifDir, ExifDirectoryBase.TAG_LENS);
                if (!lens.equals("未知")) {
                    return lens;
                }

                // 尝试 TAG_LENS_SPECIFICATION（0xA432）- 镜头规格信息
                String lensSpec = getTagValue(exifDir, ExifDirectoryBase.TAG_LENS_SPECIFICATION);
                if (!lensSpec.equals("未知")) {
                    return lensSpec;
                }
            }
        }

        // 如果所有位置都没有找到镜头信息，返回默认值
        return "未知";
    }

    /**
     * 从 EXIF 目录中安全地获取指定标签的值（通用方法）
     * 
     * 该方法使用泛型支持所有继承自 Directory 的 EXIF 目录类型，
     * 会自动检查目录和标签的有效性，避免空指针异常。
     *
     * @param directory EXIF 目录对象（可以是 IFD0、SubIFD 或其他目录）
     * @param tagType   要获取的 EXIF 标签类型常量
     * @return 标签对应的描述性字符串，如果不存在或无效则返回 "未知"
     */
    private String getTagValue(Directory directory, int tagType) {
        // 检查目录是否存在且包含指定标签
        if (directory != null && directory.containsTag(tagType)) {
            // 获取标签的描述性文本（会自动将数值转换为可读格式）
            String description = directory.getDescription(tagType);
            // 确保返回值不为空且不是空白字符串
            if (description != null && !description.isBlank()) {
                return description;
            }
        }
        // 如果无法获取有效值，返回默认提示
        return "未知";
    }
}
