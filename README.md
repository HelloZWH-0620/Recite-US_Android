<!--markdownlint-disable MD001 MD033 MD041 MD051-->

<div align="center">

# <img src="app/src/main/icon/ReciteUS.ico" height="28" width="28"/> 背书哇 (Recite-US Android)

[![Stars](https://img.shields.io/github/stars/HelloZWH-0620/Recite-US_Android?label=Stars)](https://github.com/HelloZWH-0620/Recite-US_Android)
[![版本](https://img.shields.io/badge/版本-v1.0.0_Beta-orange?style=flat-square)](https://github.com/HelloZWH-0620/Recite-US_Android/releases)
![Android 版本](https://img.shields.io/badge/Android-16-3ddc84?style=flat-square&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7f52ff?style=flat-square&logo=kotlin)
![Material 3](https://img.shields.io/badge/Material_3-原生组件-0052cc?style=flat-square)
![GitHub Repo size](https://img.shields.io/github/repo-size/HelloZWH-0620/Recite-US_Android?style=flat-square&color=3cb371)
[![GitHub Repo Languages](https://img.shields.io/github/languages/top/HelloZWH-0620/Recite-US_Android?style=flat-square)](https://github.com/HelloZWH-0620/Recite-US_Android/search?l=kotlin)

背书哇是一款帮助背诵文言文与古诗词的 Android 原生应用。<br/>
内置 60 篇必背篇目（文言文 20 篇 + 古诗词 40 篇），提供注释、译文、朗读与作者资料，陪你把每一篇背下来。

#### 💬[背书哇 QQ 交流群：950739151](https://qm.qq.com/cgi-bin/qm/qr?k=群号自行生成)

#### [⬇️ 下载 APK](https://github.com/HelloZWH-0620/Recite-US_Android/releases) | [📖 源码目录](https://github.com/HelloZWH-0620/Recite-US_Android/tree/main/Beishuwa)

</div>

## 功能

### 文章阅读

- [x] 文言文 / 古诗词双 Tab 列表，sticky header 分组显示篇名与作者
- [x] 搜索框实时过滤篇目与作者，输入即出搜索建议，点击直达详情
- [x] 正文页主标题居中、作者右对齐按钮，点击进入作者资料
- [x] 正文按开关插入译文卡片、注释卡片（灰底白字）
- [x] 开启注释时正文对应词句自动变色高亮（浅色蓝 / 深色金黄）
- [x] 文言文逐段首行缩进，词、曲仅首行缩进

### 朗读

- [x] 正文底部操作栏一键朗读
- [x] 实验性功能开启后播放内置朗读音频（人民教育出版社录音资源），由系统媒体中心 / 通知栏控制播放与暂停，播完单篇自动停止
- [x] 暂缺音频的篇目提示"正在开发中"

### 作者资料

- [x] 朝代导航（先秦 / 汉 / 魏晋南北朝 / 唐 / 五代 / 宋 / 元 / 明 / 清）与作者列表二级联动
- [x] 左侧点选定位、右侧滚动高亮，sticky header 分组
- [x] 作者详情基于内嵌 WebView + 自研 Markdown 渲染器，支持深色模式
- [x] 作者资料自动抓取自古诗文网并缓存到本地，支持复制链接 / 系统浏览器打开 / 分享

### 其它功能

- [x] 预测性返回手势跟手动画，与 Android 16 系统导航同步，逐级返回并保留滚动位置
- [x] Material 3 主题、系统字体与深色模式跟随
- [x] "我的"页：清除缓存、实验性功能开关（本地朗读 / 横屏旋转）、关于弹窗

## 开始使用

**首先，请确保您的设备满足以下要求：**

- Android 16（API 36）或更高版本的 Android 手机

对于普通用户，可以在以下渠道下载到本应用：

- [**GitHub Releases（推荐）**](https://github.com/HelloZWH-0620/Recite-US_Android/releases)

> [!IMPORTANT]
> 实验性功能（本地音频朗读、横屏旋转）默认关闭，可在【我的】->【设置】->【实验性功能】中开启。
>
> 首次使用朗读功能时，系统会申请通知权限，用于在媒体中心显示播放控制。

## 获取帮助＆加入社区

[![加入 QQ 群](https://img.shields.io/badge/QQ_%E4%BA%A4%E6%B5%81%E7%BE%A4-950739151-%230066cc?style=flat-square&logo=TencentQQ)](https://qm.qq.com/cgi-bin/qm/qr?k=群号自行生成)

如果您确定您遇到的问题是一个 **Bug**，或者您要提出一项**新的功能**，请[提交 Issue](https://github.com/HelloZWH-0620/Recite-US_Android/issues/new)。

## 开发

本项目使用 Kotlin + Jetpack Compose（Material 3）开发，目标平台 Android 16（minSdk 36 环境按需调整）。

在本地编译应用：

1. 使用 Android Studio 打开仓库根目录下的 `Beishuwa` 工程；
2. 等待 Gradle Sync 完成；
3. 运行 `app` 的 `assembleDebug` 任务即可生成 APK。

如果您有意愿为背书哇做出代码贡献，欢迎提交 [Pull Request](https://github.com/HelloZWH-0620/Recite-US_Android/pulls)。

## 致谢

本项目正文资源来自 [clover-yan/gaokao-poetry](https://github.com/clover-yan/gaokao-poetry)，录音资源来自人民教育出版社。

感谢以下同学为本项目的开发提供支持：

- 作者：[@喔糖圆鼠](https://github.com/HelloZWH-0620)
- 共创作者：@F15EX、@落星尘、@Motziki、@やわらかな光
- 鸣谢：@2friend、@Clover-Yan、[《重庆日报》](https://www.cqrb.cn/)、南昌雷式学校

本项目使用了 [Kotlin](https://kotlinlang.org/)、[Jetpack Compose](https://developer.android.com/compose)、[Coil](https://coil-kt.github.io/coil/)、[Jsoup](https://jsoup.org/) 等开源库与框架，感谢开源社区。

## 许可证

本项目基于 [GNU General Public License v3.0](LICENSE.txt) 获得许可。

<div align="center">

如果这个项目对您有帮助，请点亮 Star ⭐

</div>
