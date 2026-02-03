plugins {
    alias(libs.plugins.onodatatableAndroidLibrary)
    alias(libs.plugins.onodatatableAndroidLibraryCompose)
}

android {
    namespace = "com.onodatatable.core"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}