import org.jetbrains.compose.desktop.application.dsl.TargetFormat

// Desktop app. A placeholder window until the shared UI moves to commonMain (Phase 6-7).
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(projects.composeApp)
}

compose.desktop {
    application {
        mainClass = "io.github.raghavsatyadev.openplayer.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "OpenPlayer"
            packageVersion = "1.0.0"
        }
    }
}
