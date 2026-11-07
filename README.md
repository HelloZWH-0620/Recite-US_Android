<!--markdownlint-disable MD001 MD033 MD041 MD051-->

<div align="center">

# <img src="app/src/main/icon/ReciteUS.ico" height="28" width="28"/> 背书哇（Android）

[![Stars](https://img.shields.io/github/stars/HelloZWH-0620/Recite-US_Android?label=Stars)](https://github.com/HelloZWH-0620/Recite-US_Android)
[![版本](https://img.shields.io/badge/版本-v1.0.0_Beta-orange?style=flat-square)](https://github.com/HelloZWH-0620/Recite-US_Android/releases)
![Android 版本](https://img.shields.io/badge/Android-16-3ddc84?style=flat-square&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7f52ff?style=flat-square&logo=kotlin)
![Material 3](https://img.shields.io/badge/设计规范-Material_3-0052cc?style=flat-square)


背书哇 是一款帮助背诵文言文与古诗词的 Android 应用。<br/>
内置 60 篇必背篇目（文言文 20 篇 + 古诗词 40 篇），提供注释、译文、朗读与作者资料，致力于让全国高中生摆脱死记硬背的困境。

#### 💬 [背书哇 QQ 交流群：950739151](https://qm.qq.com/cgi-bin/qm/qr?k=群号自行生成)

#### ⬇️ [下载 APK](https://github.com/HelloZWH-0620/Recite-US_Android/releases) | [📖 源码目录](https://github.com/HelloZWH-0620/Recite-US_Android/tree/main/Beishuwa)

</div>

## 功能

### 文章阅读

- [x] 文言文 / 古诗词列表
- [x] 一键查看作者资料
- [x] 可开启译文、注释（持续优化中）


### 朗读

- [ ] 正文一键朗读（实验性功能）

### 作者资料

- [x] 朝代导航，快速检索
- [x] 作者详情基于内嵌 WebView + Markdown 渲染器

### 其它功能

- [x] 预测性返回手势跟手动画（Android 16+）
- [x] 深色模式

## 开始使用

**首先，请确保您的设备满足以下要求：**

- Android 16（API 36）或更高版本的 Android 手机

可以在以下渠道下载到本应用：

- [**GitHub Releases（推荐）**](https://github.com/HelloZWH-0620/Recite-US_Android/releases)

> [!IMPORTANT]
> 实验性功能（本地音频朗读、横屏旋转）默认关闭，可在【我的】->【设置】->【实验性功能】中开启。
>
> 首次启动应用时，系统会申请通知权限，用于在媒体中心显示播放控制按钮（实验性功能）。

## 获取帮助＆加入社区

[![加入 QQ 群](https://img.shields.io/badge/QQ_%E4%BA%A4%E6%B5%81%E7%BE%A4-950739151-%230066cc?style=flat-square&logo=TencentQQ)](https://qm.qq.com/cgi-bin/qm/qr?k=群号自行生成)

如果您确定您遇到的问题是一个 **Bug**，或者您要提出一项**新的功能**，请[提交 Issue](https://github.com/HelloZWH-0620/Recite-US_Android/issues/new)。

## 开发

本项目使用 Kotlin + Jetpack Compose（Material 3）开发，目标平台 Android 16（minSdk 36 环境按需调整）。

在本地编译应用：

1. 使用 Android Studio 打开仓库根目录下的 `Beishuwa` 工程；
2. 等待 Gradle Sync 完成；
3. 运行 `app` 的 `assembleDebug` 任务即可生成 APK。

如果您有意愿为背书哇做出代码贡献，请先阅读[贡献指南](CONTRIBUTING.md)来了解如何为背书哇做出贡献（包括资料性纠错、Bug 反馈与代码贡献）。我们欢迎想要为本应用实现新功能或进行改进的同学提交 [Pull Request](https://github.com/HelloZWH-0620/Recite-US_Android/pulls)。

## 致谢

本项目正文资源来自 [clover-yan/gaokao-poetry](https://github.com/clover-yan/gaokao-poetry)，录音资源来自人民教育出版社。

感谢以下同学为本项目的开发提供支持：

- 作者：[@喔糖圆鼠](https://github.com/HelloZWH-0620)
- 共创作者：[@F15EX](https://github.com/ClassTechStar)、@落星尘、[@やわらかな光](https://github.com/TTStudio336)
- 鸣谢：@2friend、@Clover-Yan、[《重庆日报》](https://www.cqrb.cn/)、南昌雷式学校

本项目使用了 [Kotlin](https://kotlinlang.org/)、[Jetpack Compose](https://developer.android.com/compose)、[Coil](https://coil-kt.github.io/coil/)、[Jsoup](https://jsoup.org/) 等开源库与框架。

## 许可证

本项目基于 [GNU General Public License v3.0](LICENSE.txt) 获得许可。

<div align="center">

如果这个项目对您有帮助，欢迎点亮 Star ⭐
</div>

![Star History Chart](https://shieldcn.dev/chart/github/stars/HelloZWH-0620/Recit_US_Android.svg)


