This is a Kotlin Multiplatform project targeting Android, Desktop (JVM).

English | [中文](README.zh.md)

> ### Warning
> there are clearly evidences show that this program has memory leak problem in desktop, maybe exist in android else.

### Program Structure

* [/composeApp](./composeApp/src) is the main code of application
* [/plugin/ech-request](./plugin/ech-request) is direct connection solution of some websites.

### Build Environment Require

Rust + Android NDK + JDK 17~

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

### Reference
* [HachimiWorldClient](https://github.com/HachimiWorld/hachimi-world-client)
* [EhViewer(With direct connection)](https://github.com/UjuiUjuMandan/EhViewer-NekoInverter)