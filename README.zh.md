本项目基于Kotlin Multiplatform框架，适用于桌面端与安卓端

[English](README.md) | 中文

> ### 警告
> 目前有足够的测试表明项目桌面端存在内存泄漏的问题，这也许也存在于安卓端

### 构建环境需求

Rust + Android NDK + JDK 17~

### 项目结构

* [/composeApp](./composeApp/src) 是软件的主要代码
* [/plugin/ech-request](./plugin/ech-request) 是本项目保障部分网络请求隐私安全的解决方案

### 构建并运行安卓端应用

构建并运行安卓端开发版，可在 IDE 工具栏的运行控件中选择运行配置，也可直接在终端构建：

- macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### 构建并运行桌面端（JVM）应用

构建并运行桌面端开发版，可在 IDE 工具栏的运行控件中选择运行配置，也可直接在终端运行：

- macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

---

了解 [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) 的更多内容…

### 参考
* [HachimiWorldClient](https://github.com/HachimiWorld/hachimi-world-client)
* [EhViewer(直连版)](https://github.com/UjuiUjuMandan/EhViewer-NekoInverter)