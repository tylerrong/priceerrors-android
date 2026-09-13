plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

fun releaseSetting(name: String): String? =
    providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orNull
        ?.takeIf(String::isNotBlank)

fun buildSetting(name: String, defaultValue: String = ""): String =
    releaseSetting(name) ?: defaultValue

fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val releaseStoreFile = releaseSetting("PRICEERRORS_RELEASE_STORE_FILE")
val releaseStorePassword = releaseSetting("PRICEERRORS_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseSetting("PRICEERRORS_RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseSetting("PRICEERRORS_RELEASE_KEY_PASSWORD")
val releaseSigningValues = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
)
val hasAnyReleaseSigningValue = releaseSigningValues.any { it != null }
val hasCompleteReleaseSigning = releaseSigningValues.all { it != null }

check(!hasAnyReleaseSigningValue || hasCompleteReleaseSigning) {
    "Release signing is only partially configured. Set all four PRICEERRORS_RELEASE_* values " +
        "or remove them all to produce an unsigned local release bundle."
}

// A release build with no console values compiles and packages perfectly well —
// it is only unusable once installed, because release disables the debug email
// path and Google sign-in needs a client ID. That failure is invisible until a
// tester or reviewer hits it, so check the shippable config up front instead.
//
// Pass -PPRICEERRORS_ALLOW_INCOMPLETE_RELEASE=true to build an unsigned release
// anyway, which is what the R8/lint validation pass in the README wants.
val producesReleaseArtifact = gradle.startParameter.taskNames.any { requested ->
    when (requested.substringAfterLast(':')) {
        "assembleRelease",
        "bundleRelease",
        "assembleClosedTest",
        "bundleClosedTest",
        "assemble",
        "bundle",
        "build",
        -> true
        else -> false
    }
}
val allowIncompleteRelease =
    releaseSetting("PRICEERRORS_ALLOW_INCOMPLETE_RELEASE")?.toBoolean() ?: false

if (producesReleaseArtifact) {
    val backendKeys = listOf(
        "PRICEERRORS_SERVER_BASE_URL",
        "PRICEERRORS_SERVER_API_KEY",
        "PRICEERRORS_SUPABASE_URL",
        "PRICEERRORS_SUPABASE_ANON_KEY",
    )
    val blockers = buildList {
        if (buildSetting("PRICEERRORS_GOOGLE_WEB_CLIENT_ID").isBlank()) {
            add(
                "PRICEERRORS_GOOGLE_WEB_CLIENT_ID is missing. Release builds disable the debug " +
                    "email path, so without it the app has no way to sign in at all.",
            )
        }
        val missingBackend = backendKeys.filter { buildSetting(it).isBlank() }
        if (missingBackend.isNotEmpty()) {
            add(
                "Backend config is incomplete (${missingBackend.joinToString()}). The app would " +
                    "ship against the built-in sample feed.",
            )
        }
    }

    val warnings = buildList {
        val firebaseKeys = listOf(
            "PRICEERRORS_FIREBASE_APPLICATION_ID",
            "PRICEERRORS_FIREBASE_API_KEY",
            "PRICEERRORS_FIREBASE_PROJECT_ID",
            "PRICEERRORS_FIREBASE_SENDER_ID",
        )
        if (firebaseKeys.any { buildSetting(it).isBlank() }) {
            add("Firebase is not configured — push alerts will be silently unavailable.")
        }
        if (!hasCompleteReleaseSigning) {
            add("No PRICEERRORS_RELEASE_* signing values — the output will be unsigned.")
        }
    }

    warnings.forEach { logger.warn("w: release config — $it") }

    if (blockers.isNotEmpty()) {
        val detail = blockers.joinToString("\n") { "  - $it" }
        if (allowIncompleteRelease) {
            logger.warn("w: release config problems ignored via PRICEERRORS_ALLOW_INCOMPLETE_RELEASE:\n$detail")
        } else {
            error(
                "This release build would not be shippable:\n$detail\n\n" +
                    "Set the values as Gradle properties (~/.gradle/gradle.properties) or CI " +
                    "environment variables. To build anyway for R8/lint validation, pass " +
                    "-PPRICEERRORS_ALLOW_INCOMPLETE_RELEASE=true.",
            )
        }
    }
}

android {
    namespace = "app.priceerrors"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.priceerrors"
        minSdk = 26
        targetSdk = 37
        versionCode = releaseSetting("PRICEERRORS_VERSION_CODE")?.toInt() ?: 1
        versionName = releaseSetting("PRICEERRORS_VERSION_NAME") ?: "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            buildConfigString(buildSetting("PRICEERRORS_GOOGLE_WEB_CLIENT_ID")),
        )
        buildConfigField(
            "String",
            "SERVER_BASE_URL",
            buildConfigString(buildSetting("PRICEERRORS_SERVER_BASE_URL")),
        )
        buildConfigField(
            "String",
            "SERVER_API_KEY",
            buildConfigString(buildSetting("PRICEERRORS_SERVER_API_KEY")),
        )
        buildConfigField(
            "String",
            "SUPABASE_URL",
            buildConfigString(buildSetting("PRICEERRORS_SUPABASE_URL")),
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            buildConfigString(buildSetting("PRICEERRORS_SUPABASE_ANON_KEY")),
        )
        buildConfigField(
            "String",
            "BILLING_WEEKLY_PRODUCT_ID",
            buildConfigString(buildSetting("PRICEERRORS_BILLING_WEEKLY_PRODUCT_ID", "priceerrors_pro_weekly")),
        )
        buildConfigField(
            "String",
            "BILLING_MONTHLY_PRODUCT_ID",
            buildConfigString(buildSetting("PRICEERRORS_BILLING_MONTHLY_PRODUCT_ID", "priceerrors_pro_monthly")),
        )
        buildConfigField(
            "String",
            "BILLING_YEARLY_PRODUCT_ID",
            buildConfigString(buildSetting("PRICEERRORS_BILLING_YEARLY_PRODUCT_ID", "priceerrors_pro_yearly")),
        )
        buildConfigField(
            "String",
            "BILLING_RESCUE_PRODUCT_ID",
            buildConfigString(buildSetting("PRICEERRORS_BILLING_RESCUE_PRODUCT_ID", "priceerrors_pro_monthly_rescue")),
        )
        buildConfigField(
            "String",
            "FIREBASE_APPLICATION_ID",
            buildConfigString(buildSetting("PRICEERRORS_FIREBASE_APPLICATION_ID")),
        )
        buildConfigField(
            "String",
            "FIREBASE_API_KEY",
            buildConfigString(buildSetting("PRICEERRORS_FIREBASE_API_KEY")),
        )
        buildConfigField(
            "String",
            "FIREBASE_PROJECT_ID",
            buildConfigString(buildSetting("PRICEERRORS_FIREBASE_PROJECT_ID")),
        )
        buildConfigField(
            "String",
            "FIREBASE_SENDER_ID",
            buildConfigString(buildSetting("PRICEERRORS_FIREBASE_SENDER_ID")),
        )
        buildConfigField(
            "String",
            "FACEBOOK_APP_ID",
            buildConfigString(buildSetting("PRICEERRORS_FACEBOOK_APP_ID")),
        )
        buildConfigField(
            "String",
            "FACEBOOK_CLIENT_TOKEN",
            buildConfigString(buildSetting("PRICEERRORS_FACEBOOK_CLIENT_TOKEN")),
        )
        buildConfigField(
            "String",
            "POSTHOG_API_KEY",
            buildConfigString(buildSetting("PRICEERRORS_POSTHOG_API_KEY")),
        )
        buildConfigField(
            "String",
            "POSTHOG_HOST",
            buildConfigString(
                buildSetting("PRICEERRORS_POSTHOG_HOST", "https://us.i.posthog.com"),
            ),
        )
        // The dedicated closed-test artifact grants temporary full access while
        // Play Console monetization is unavailable. Normal release builds keep
        // this false so the production paywall cannot be disabled accidentally.
        buildConfigField("boolean", "CLOSED_TEST_FREE_ACCESS", "false")
    }

    signingConfigs {
        if (hasCompleteReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("closedTest") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "CLOSED_TEST_FREE_ACCESS", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.id)
    implementation(libs.play.billing)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.facebook.core)
    implementation(libs.posthog.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.play.review.ktx)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
