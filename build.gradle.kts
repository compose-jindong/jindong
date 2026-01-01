plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    alias(libs.plugins.spotless) apply false
}

subprojects {
    apply(plugin = "com.diffplug.spotless")

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**/*.kt")
            ktlint("1.8.0")
                .editorConfigOverride(
                    mapOf(
                        "indent_size" to "2",
                        "continuation_indent_size" to "2",
                        "ktlint_standard_no-wildcard-imports" to "disabled",
                        "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                        "ktlint_standard_property-naming" to "disabled",
                        "ktlint_standard_function-naming" to "disabled",
                        "ktlint_standard_backing-property-naming" to "disabled"
                    )
                )
            licenseHeaderFile(rootProject.file("spotless/copyright.kt"))
            trimTrailingWhitespace()
            endWithNewline()
        }
        format("kts") {
            target("**/*.kts")
            targetExclude("**/build/**/*.kts")
            licenseHeaderFile(rootProject.file("spotless/copyright.kt"), "(^(?![\\/ ]\\*).*$)")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}
