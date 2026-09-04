plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val arm64Only = project.findProperty("arm64Only") == "true"

android {
    namespace = "com.denggl2.masonremote"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.denggl2.masonremote"
        minSdk = 26
        targetSdk = 36
        versionCode = 25
        versionName = "0.1.24"
    }

    buildFeatures {
        buildConfig = true
    }

    splits {
        abi {
            isEnable = arm64Only
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

android.applicationVariants.all {
    outputs.all {
        val apkOutput = this as com.android.build.gradle.api.ApkVariantOutput
        val abi = apkOutput.getFilter(com.android.build.VariantOutput.FilterType.ABI) ?: "universal"
        val version = mergedFlavor.versionName ?: "dev"
        apkOutput.outputFileName = "CloudX-$version-$abi-${buildType.name}.apk"
    }
}

val apkArchiveDir = rootProject.layout.projectDirectory.dir("artifacts/apks").asFile

listOf("Debug", "Release").forEach { variantSuffix ->
    val variantName = variantSuffix.lowercase()
    val archiveTask = tasks.register("archive${variantSuffix}Apk") {
        dependsOn("package$variantSuffix")
        doLast {
            val outputDir = layout.buildDirectory.dir("outputs/apk/$variantName").get().asFile
            val apkFiles = outputDir.listFiles { file ->
                file.isFile && file.extension == "apk" && file.name.startsWith("CloudX-")
            }.orEmpty()
            check(apkFiles.isNotEmpty()) {
                "No CloudX APK found in ${outputDir.absolutePath}"
            }

            apkArchiveDir.mkdirs()
            apkFiles.forEach { source ->
                var destination = apkArchiveDir.resolve(source.name)
                var buildNumber = 2
                while (destination.exists()) {
                    destination = apkArchiveDir.resolve(
                        "${source.nameWithoutExtension}-build-$buildNumber.apk",
                    )
                    buildNumber += 1
                }
                source.copyTo(destination, overwrite = false)
                logger.lifecycle("Archived APK: ${destination.absolutePath}")
            }
        }
    }

    tasks.configureEach {
        if (name == "assemble$variantSuffix") {
            finalizedBy(archiveTask)
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.okhttp)
    implementation("io.github.webrtc-sdk:android:125.6422.07")
    implementation(libs.kotlinx.serialization.json)
    implementation("dev.chrisbanes.haze:haze:1.1.1")

    debugImplementation(libs.compose.ui.tooling)
}
