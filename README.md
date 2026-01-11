# 摄影参数海报生成器

一个基于 Spring Boot 的摄影海报生成工具，自动提取照片 EXIF 数据并生成精美的摄影参数海报。

## ✨ 功能特性

- 📷 **自动识别 EXIF** - 智能提取相机型号、镜头、光圈、快门、ISO 等参数
- 🎨 **多模板支持** - 经典白底、毛玻璃背景等多种风格
- 🖼️ **高清无损导出** - 支持 PNG/JPG 格式，保持原图画质
- 📦 **批量处理** - 支持多图同时处理，自动打包 ZIP 下载
- 🔄 **自动旋转** - 根据 EXIF Orientation 自动校正竖拍照片
- 🎯 **智能对比度** - 根据背景亮度自动调整文字颜色

## 🛠️ 技术栈

- **后端**: Spring Boot 4.0, Java 21
- **图像处理**: JH Labs Image Filters, Java AWT/Graphics2D
- **EXIF 提取**: metadata-extractor
- **前端**: Thymeleaf, Vanilla CSS/JS

## 🚀 快速开始

### 环境要求

- JDK 21+
- Gradle 8+

### 运行项目

```bash
# 克隆项目
git clone https://github.com/你的用户名/photo-poster-generator.git
cd photo-poster-generator

# 运行
./gradlew bootRun
```

访问 http://localhost:8081

## 📖 使用说明

1. 打开首页，选择 **单张处理** 或 **批量处理** 模式
2. 上传照片（支持 JPEG/PNG，最大 200MB）
3. 选择模板样式和导出格式
4. 点击生成并下载海报

## 📁 项目结构

```
src/main/java/yuan/demo/
├── controller/          # 控制器
├── service/             # 服务层
├── template/            # 海报模板
└── dto/                 # 数据传输对象

src/main/resources/
├── static/              # 静态资源
└── templates/           # Thymeleaf 模板
```

## 📝 License

MIT License
