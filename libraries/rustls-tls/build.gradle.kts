import extension.buildConfigFieldBoolean
import extension.testCommonDependencies

plugins {
    id("io.element.android-library")
}

android {
    namespace = "org.rustls.platformverifier"
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigFieldBoolean("TEST", false)
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(libs.timber)
    testCommonDependencies(libs)
}
