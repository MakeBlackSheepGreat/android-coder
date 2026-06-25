# Android APK 构建指南

## 方法一：使用 Android Studio（推荐）

### 步骤：

1. **打开项目**
   - 启动 Android Studio
   - File → Open → 选择项目根目录

2. **同步 Gradle**
   - 打开后会自动同步
   - 或点击 "Sync Project with Gradle Files" 按钮

3. **构建 APK**
   - 点击菜单: Build → Build Bundle(s) / APK(s) → Build APK(s)
   - 等待构建完成
   - 点击通知中的 "locate" 查看 APK 位置

4. **APK 位置**
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 方法二：使用命令行

### Windows (Git Bash 或 PowerShell)

```bash
# 1. 进入项目根目录
cd C:\Users\876762330\Desktop\projects\SWUST-Code\android-coder

# 2. 构建 Debug APK
./gradlew assembleDebug

# 3. APK 位置
# app/build/outputs/apk/debug/app-debug.apk
```

### Linux/macOS

```bash
# 1. 进入项目根目录
cd /path/to/android-coder

# 2. 赋予执行权限（首次）
chmod +x gradlew

# 3. 构建 Debug APK
./gradlew assembleDebug

# 4. APK 位置
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 方法三：构建 Release APK（需要签名）

### 创建签名密钥

```bash
keytool -genkey -v -keystore coder-release.keystore -alias coder -keyalg RSA -keysize 2048 -validity 10000
```

### 配置签名

在 `app/build.gradle.kts` 中添加：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../coder-release.keystore")
            storePassword = "your-store-password"
            keyAlias = "coder"
            keyPassword = "your-key-password"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### 构建 Release APK

```bash
./gradlew assembleRelease
```

APK 位置: `app/build/outputs/apk/release/app-release.apk`

---

## 常见问题

### 1. Gradle 下载慢

**解决方案**：使用国内镜像

编辑 `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
    }
}
```

### 2. 构建失败

```bash
# 清理构建缓存
./gradlew clean

# 重新构建
./gradlew assembleDebug --stacktrace
```

### 3. 内存不足

编辑 `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

---

## 快速构建脚本

创建 `build-apk.sh`:

```bash
#!/bin/bash

echo "🚀 开始构建 APK..."

# 清理
./gradlew clean

# 构建
./gradlew assembleDebug

# 检查结果
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ 构建成功！"
    echo "📦 APK 位置: app/build/outputs/apk/debug/app-debug.apk"
    
    # 显示文件大小
    ls -lh app/build/outputs/apk/debug/app-debug.apk
else
    echo "❌ 构建失败"
    exit 1
fi
```

使用方法：

```bash
chmod +x build-apk.sh
./build-apk.sh
```

---

## 使用 GitHub Actions 自动构建

创建 `.github/workflows/build-apk.yml`:

```yaml
name: Build APK

on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Build Debug APK
      run: ./gradlew assembleDebug
      
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

推送到 GitHub 后，在 Actions 标签页下载构建的 APK。

---

## 系统要求

### 开发环境
- **JDK**: 17 或更高
- **Android SDK**: API 26-35
- **Gradle**: 8.13.2（自动下载）
- **Kotlin**: 2.3.21

### 最低设备要求
- **Android**: 8.0 (API 26) 或更高
- **RAM**: 2GB+
- **存储**: 50MB+

---

## APK 信息

### Debug APK
- **包名**: com.dlzz.coder
- **版本**: 0.1.0
- **大小**: ~10-15 MB（未优化）
- **签名**: Debug 签名（不可发布）

### Release APK
- **包名**: com.dlzz.coder
- **版本**: 0.1.0
- **大小**: ~5-8 MB（ProGuard 优化后）
- **签名**: 需要自己的签名密钥

---

## 安装 APK

### 通过 ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 直接安装
1. 将 APK 传输到 Android 设备
2. 在设备上打开文件管理器
3. 点击 APK 文件
4. 允许"未知来源"安装（如需要）
5. 点击"安装"

---

## 下一步

1. ✅ 运行上述任一方法构建 APK
2. ✅ 测试 APK 功能
3. ✅ 如需发布，配置 Release 签名
4. ✅ 考虑设置 CI/CD 自动构建

**提示**: Debug APK 仅用于测试，发布到用户请使用 Release APK。
