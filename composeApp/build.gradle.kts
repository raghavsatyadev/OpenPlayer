import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Shared app code. Android code lives in androidMain for now; it moves to commonMain
// piece by piece, with Android-only features behind interfaces (see .claude/CMP_MIGRATION.md).
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room3)
    id("org.jetbrains.kotlin.plugin.parcelize")
}

val enableComposeCompilerReports = providers.gradleProperty("openplayer.enableComposeCompilerReports")
    .getOrElse("false")
    .toBoolean()

room3 {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    android {
        // Kept equal to the old :app namespace so every existing R import still resolves.
        namespace = "io.github.raghavsatyadev.openplayer"
        compileSdk {
            version = release(37) {
                minorApiLevel = 1
            }
        }
        minSdk = 30
        androidResources.enable = true
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
    }

    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }

    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
        freeCompilerArgs.add("-Xexpect-actual-classes")

        if (enableComposeCompilerReports) {
            val buildDir = project.layout.buildDirectory.get().asFile.absolutePath
            freeCompilerArgs.addAll(
                "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$buildDir/compose_compiler_reports",
                "-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$buildDir/compose_compiler_metrics"
            )
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.jetbrains.compose.runtime)
            api(libs.jetbrains.compose.ui)
            api(libs.jetbrains.compose.components.resources)
        }
        androidMain.dependencies {
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.documentfile)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.lifecycle.runtime.compose)
        implementation(libs.lifecycleprocess)
        implementation(libs.androidx.activity.compose)
        implementation(project.dependencies.platform(libs.androidx.compose.bom))
        implementation(libs.androidx.ui)
        implementation(libs.androidx.ui.graphics)
        implementation(libs.androidx.ui.tooling.preview)
        implementation(libs.androidx.compose.material3)
        implementation(libs.androidx.compose.material3.adaptive)
        implementation(libs.androidx.constraintlayout.compose)
        implementation(libs.androidx.foundation)
        implementation(libs.androidx.animation)
        implementation(libs.androidx.palette.ktx)
        implementation(libs.androidx.core.splashscreen)
        implementation(libs.material)
        implementation(libs.androidx.appcompat)

        api(project.dependencies.platform(libs.koin.bom))
        api(libs.koin.core)
        implementation(libs.koin.android)
        implementation(libs.koin.androidx.compose)
        implementation(libs.koin.androidx.workmanager)
        implementation(libs.koin.compose.viewmodel)
        implementation(libs.androidx.navigation.compose)
        implementation(libs.androidx.navigation.runtime.ktx)

        implementation(libs.androidx.room3.runtime)
        implementation(libs.androidx.room3.paging)
        implementation(libs.androidx.sqlite.bundled)
        implementation(libs.androidx.paging.runtime)
        implementation(libs.androidx.paging.compose)
        implementation(libs.androidx.paging.common)

        implementation(libs.androidx.media3.exoplayer)
        implementation(libs.androidx.media3.ui)
        implementation(libs.androidx.media3.session)
        implementation(libs.androidx.media3.exoplayer.ffmpeg)
        implementation(libs.androidx.media3.exoplayer.midi)
        implementation(libs.androidx.media3.transformer)
        implementation(libs.androidx.media)
        api(libs.coil.compose)
        implementation(libs.coil.network.okhttp)
        implementation(libs.taglib)
        implementation(libs.jaudiotagger)
        implementation(libs.vorbisjava.core)
        implementation(libs.wavy.slider)
        implementation(libs.androidx.graphics.shapes)

        implementation(libs.retrofit)
        implementation(libs.converter.gson)
        implementation(libs.okhttp)
        implementation(libs.logging.interceptor)
        implementation(libs.gson)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.snakeyaml)
        implementation(libs.kotlinx.collections.immutable)
        implementation(libs.ktor.server.core)
        implementation(libs.ktor.server.cio)

        implementation(libs.androidx.work.runtime.ktx)
        implementation(libs.androidx.security.crypto)

        implementation(libs.kermit)
        implementation(libs.smooth.corner.rect.android.compose)
        implementation(libs.reorderables)
        implementation(libs.androidx.glance)
        implementation(libs.androidx.glance.appwidget)
        implementation(libs.androidx.glance.material3)
        implementation(libs.kuromoji.ipadic)
        implementation(libs.pinyin4j.core)
        implementation(libs.accompanist.drawablepainter)
        implementation(libs.accompanist.permissions)
        implementation(libs.capturable.get().let { "${it.module}:${it.versionConstraint.requiredVersion}" }) {
            exclude(group = "androidx.compose.animation")
            exclude(group = "androidx.compose.foundation")
            exclude(group = "androidx.compose.material")
            exclude(group = "androidx.compose.runtime")
            exclude(group = "androidx.compose.ui")
        }
        }
    }
}

compose.resources {
    packageOfResClass = "io.github.raghavsatyadev.openplayer.resources"
    publicResClass = true
}

composeCompiler {
    stabilityConfigurationFiles.add(project.layout.projectDirectory.file("compose_stability.conf"))
}

dependencies {
    add("kspAndroid", libs.androidx.room3.compiler)
}
