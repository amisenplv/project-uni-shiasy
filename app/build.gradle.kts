plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.unishiasy.vehiclelog"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "org.amirariana.vehiclelog"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("boolean", "LOCAL_CLOUD_ENABLED", "false")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
            applicationIdSuffix = ".debug"
            buildConfigField(
                "boolean", "LOCAL_CLOUD_ENABLED",
                (providers.gradleProperty("vehiclelogLocalCloud").orNull == "true").toString(),
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets.getByName("androidTest").assets.srcDir("schemas")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

androidComponents.onVariants(androidComponents.selector().withBuildType("release")) { variant ->
    val requestedCode = providers.gradleProperty("vehiclelogReleaseVersionCode").orNull
    val requestedName = providers.gradleProperty("vehiclelogReleaseVersionName").orNull
    if (requestedCode != null || requestedName != null) {
        val versionCode = requestedCode?.toIntOrNull()
        require(versionCode != null && versionCode in 1..2_100_000_000 &&
            versionCode.toString() == requestedCode) {
            "Set vehiclelogReleaseVersionCode to an integer from 1 to 2100000000 without leading zeros."
        }
        require(requestedName != null && requestedName.matches(Regex("[A-Za-z0-9][A-Za-z0-9._+-]{0,63}"))) {
            "Set vehiclelogReleaseVersionName to a 1–64 character ASCII release label."
        }
        variant.outputs.forEach { output ->
            output.versionCode.set(versionCode)
            output.versionName.set(requestedName)
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    // The key/value overload preserves spaces in the schema path on KSP 2.3.6.
    arg("room.schemaLocation", layout.projectDirectory.dir("schemas").asFile.path)
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
}
