import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val skyPortalStoreFile = providers.gradleProperty("skyportalStoreFile")
    .orElse(providers.environmentVariable("SKYPORTAL_STORE_FILE"))
val skyPortalStorePassword = providers.gradleProperty("skyportalStorePassword")
    .orElse(providers.environmentVariable("SKYPORTAL_STORE_PASSWORD"))
val skyPortalKeyAlias = providers.gradleProperty("skyportalKeyAlias")
    .orElse(providers.environmentVariable("SKYPORTAL_KEY_ALIAS"))
val skyPortalKeyPassword = providers.gradleProperty("skyportalKeyPassword")
    .orElse(providers.environmentVariable("SKYPORTAL_KEY_PASSWORD"))
val hasReleaseSigning = listOf(
    skyPortalStoreFile,
    skyPortalStorePassword,
    skyPortalKeyAlias,
    skyPortalKeyPassword
).all { it.isPresent }

android {
    namespace = "com.skyportalthor.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.skyportalthor.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "0.5.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("skyPortalRelease") {
                storeFile = file(skyPortalStoreFile.get())
                storePassword = skyPortalStorePassword.get()
                keyAlias = skyPortalKeyAlias.get()
                keyPassword = skyPortalKeyPassword.get()
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.findByName("skyPortalRelease")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.02.01"))
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260719")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
