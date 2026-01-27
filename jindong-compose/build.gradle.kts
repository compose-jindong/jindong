/*
 * Copyright (C) 2026 compose-jindong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
    alias(libs.plugins.binaryCompatibilityValidator)
}

apiValidation {
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}

group = "io.github.compose-jindong"
version = "1.0.0"

kotlin {
    androidLibrary {
        namespace = "io.github.compose.jindong"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":jindong-core"))
            implementation(libs.compose.runtime)
            implementation(libs.kotlinx.coroutines.core)
        }

        androidMain.dependencies {
            val composeBom = project.dependencies.platform(libs.androidx.compose.bom)
            implementation(composeBom)
            implementation(libs.androidx.compose.ui)
        }

        commonTest.dependencies {
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "jindong-compose", version.toString())

    pom {
        name = "Jindong Compose"
        description = "Declarative Haptic Feedback Library using Compose Runtime for Kotlin Multiplatform"
        inceptionYear = "2026"
        url = "https://github.com/compose-jindong/jindong"
        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
            }
        }
        developers {
            developer {
                id = "l2hyunwoo"
                name = "Hyunwoo Lee"
            }
            developer {
                id = "wisemuji"
                name = "Suhyeon Kim"
            }
        }
        scm {
            url = "https://github.com/compose-jindong/jindong"
            connection = "scm:git:git://github.com/compose-jindong/jindong.git"
            developerConnection = "scm:git:ssh://github.com/compose-jindong/jindong.git"
        }
    }
}
