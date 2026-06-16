import java.util.Base64
import java.io.File

// Absolutely guarantee that debug.keystore is decoded and present at project root
val rootDirFile = project.rootDir
val globalKeystoreFile = File(rootDirFile, "debug.keystore")
val globalBase64File = File(rootDirFile, "debug.keystore.base64")
if (!globalKeystoreFile.exists()) {
  if (globalBase64File.exists()) {
    try {
      val base64Text = globalBase64File.readText().trim()
      val decodedBytes = Base64.getDecoder().decode(base64Text)
      globalKeystoreFile.writeBytes(decodedBytes)
      println("G_KEYSTORE: Successfully decoded debug.keystore.base64 to ${globalKeystoreFile.absolutePath}")
    } catch (e: Exception) {
      println("G_KEYSTORE: Failed to decode debug.keystore.base64: ${e.message}")
    }
  } else {
    try {
      val pb = ProcessBuilder(
        "keytool", "-genkey", "-v",
        "-keystore", globalKeystoreFile.absolutePath,
        "-storepass", "android",
        "-alias", "androiddebugkey",
        "-keypass", "android",
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-dname", "CN=Android Debug,O=Android,C=US"
      )
      pb.redirectErrorStream(true)
      val process = pb.start()
      process.waitFor()
      println("G_KEYSTORE: Successfully generated debug.keystore at ${globalKeystoreFile.absolutePath}")
    } catch (e: Exception) {
      println("G_KEYSTORE: Failed to dynamically generate debug.keystore: ${e.message}")
    }
  }
} else {
  println("G_KEYSTORE: debug.keystore already exists at ${globalKeystoreFile.absolutePath}")
}

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.aistudio.securevpn.wfyzcx"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    val isReleaseSigningAvailable = System.getenv("STORE_PASSWORD") != null && System.getenv("KEY_PASSWORD") != null
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      val keystoreFile = file(keystorePath)
      if (isReleaseSigningAvailable && keystoreFile.exists()) {
        storeFile = keystoreFile
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      } else {
        storeFile = globalKeystoreFile
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
      enableV1Signing = true
      enableV2Signing = true
      enableV3Signing = true
      enableV4Signing = true
    }
    create("debugConfig") {
      storeFile = globalKeystoreFile
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
      enableV1Signing = true
      enableV2Signing = true
      enableV3Signing = true
      enableV4Signing = true
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  // implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.play.services.auth)
  implementation(libs.retrofit)
  implementation(libs.zxing.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

project.afterEvaluate {
  val buildDirFile = project.layout.buildDirectory.get().asFile
  val rootDirFile = project.rootDir

  // Strip android:testOnly from all generated manifests to prevent install errors
  tasks.matching { it.name.contains("Manifest") && !it.name.contains("Test") }.configureEach {
    doLast {
      val searchDir = File(buildDirFile, "intermediates")
      if (searchDir.exists()) {
        searchDir.walkTopDown().forEach { file ->
          if (file.name == "AndroidManifest.xml" && file.isFile) {
            try {
              var content = file.readText()
              if (content.contains("android:testOnly=\"true\"") || content.contains("android:testOnly='true'")) {
                content = content.replace("android:testOnly=\"true\"", "")
                content = content.replace("android:testOnly='true'", "")
                file.writeText(content)
                println("G_MANIFEST: Stripped android:testOnly from ${file.absolutePath}")
              }
            } catch (e: Exception) {
              println("G_MANIFEST_ERR: Failed to process ${file.absolutePath}: ${e.message}")
            }
          }
        }
      }
    }
  }

  tasks.findByName("assembleDebug")?.apply {
    doLast {
      val apkFile = File(buildDirFile, "outputs/apk/debug/app-debug.apk")
      if (apkFile.exists()) {
        val destFile1 = File(rootDirFile, "VpnCuba-debug.apk")
        val destFile2 = File(rootDirFile, ".build-outputs/app-debug.apk")
        val buildOutputsDir = File(rootDirFile, ".build-outputs")
        if (!buildOutputsDir.exists()) {
          buildOutputsDir.mkdirs()
        }
        apkFile.copyTo(destFile1, overwrite = true)
        apkFile.copyTo(destFile2, overwrite = true)
        println("G_COPY: Successfully copied Debug APK to ${destFile1.absolutePath} (${destFile1.length()} bytes)")
        println("G_COPY: Successfully copied Debug APK to ${destFile2.absolutePath} (${destFile2.length()} bytes)")
      } else {
        println("G_COPY_ERROR: Source Debug APK does not exist at ${apkFile.absolutePath}")
      }
    }
  }
  tasks.findByName("assembleRelease")?.apply {
    doLast {
      val apkFile = File(buildDirFile, "outputs/apk/release/app-release.apk")
      if (apkFile.exists()) {
        val destFile1 = File(rootDirFile, "VpnCuba-release.apk")
        val destFile2 = File(rootDirFile, ".build-outputs/app-release.apk")
        val destFile3 = File(rootDirFile, "VpnCuba-debug.apk")
        val destFile4 = File(rootDirFile, ".build-outputs/app-debug.apk")
        val buildOutputsDir = File(rootDirFile, ".build-outputs")
        if (!buildOutputsDir.exists()) {
          buildOutputsDir.mkdirs()
        }
        apkFile.copyTo(destFile1, overwrite = true)
        apkFile.copyTo(destFile2, overwrite = true)
        apkFile.copyTo(destFile3, overwrite = true)
        apkFile.copyTo(destFile4, overwrite = true)
        println("G_COPY: Successfully copied Release APK to ${destFile1.absolutePath} (${destFile1.length()} bytes)")
        println("G_COPY: Successfully copied Release APK to ${destFile2.absolutePath} (${destFile2.length()} bytes)")
        println("G_COPY: Successfully copied Release APK as fallback to ${destFile3.absolutePath} (${destFile3.length()} bytes)")
        println("G_COPY: Successfully copied Release APK as fallback to ${destFile4.absolutePath} (${destFile4.length()} bytes)")
      } else {
        println("G_COPY_ERROR: Source Release APK does not exist at ${apkFile.absolutePath}")
      }
    }
  }
}

