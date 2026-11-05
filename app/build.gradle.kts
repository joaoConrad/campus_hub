plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("jacoco")
}

android {
    namespace = "com.example.campushub"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.campushub"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }

        debug {
            enableUnitTestCoverage = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.google.firebase:firebase-analytics")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    testImplementation("io.mockk:mockk:1.13.13")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.7.0")
}

val exclusoesCobertura = listOf(
    "**/*Activity*.*",
    "**/*Adapter*.*",
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/databinding/**",
    "**/*Repository*.*",
)

val classesCobertura = fileTree(
    layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
) {
    exclude(exclusoesCobertura)
}

val dadosCobertura = layout.buildDirectory.file(
    "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
)

tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(classesCobertura)
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(dadosCobertura)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.register<JacocoCoverageVerification>("jacocoCoverageCheck") {
    group = "verification"
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(classesCobertura)
    executionData.setFrom(dadosCobertura)
    violationRules {
        rule {
            limit {
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}