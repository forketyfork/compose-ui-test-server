plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    `maven-publish`
    signing
}

group = "io.github.forketyfork"
version = "0.1.0"

kotlin {
    jvm()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.ui.InternalComposeUiApi",
            "-opt-in=androidx.compose.ui.test.ExperimentalTestApi",
        )
    }

    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    sourceSets {
        jvmMain.dependencies {
            compileOnly(compose.uiTest)
            compileOnly(compose.desktop.common)

            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.netty)
            implementation(libs.ktor.server.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(compose.uiTest)
            implementation(compose.desktop.currentOs)
        }
    }
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        withType<MavenPublication> {
            artifact(javadocJar)

            pom {
                name.set("Compose UI Test Server")
                description.set("HTTP server for controlling Compose Desktop applications at runtime, designed for AI coding agents and automation tools")
                url.set("https://github.com/forketyfork/compose-ui-test-server")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("forketyfork")
                        name.set("Sergei Petunin")
                        email.set("sergei.petunin@gmail.com")
                    }
                }

                scm {
                    url.set("https://github.com/forketyfork/compose-ui-test-server")
                    connection.set("scm:git:git://github.com/forketyfork/compose-ui-test-server.git")
                    developerConnection.set("scm:git:ssh://github.com/forketyfork/compose-ui-test-server.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

            credentials {
                username = project.findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
                password = project.findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    val signingKey = project.findProperty("signing.key") as String? ?: System.getenv("SIGNING_KEY")
    val signingPassword = project.findProperty("signing.password") as String? ?: System.getenv("SIGNING_PASSWORD")

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }

    sign(publishing.publications)
}

tasks.withType<Sign>().configureEach {
    onlyIf { !version.toString().endsWith("SNAPSHOT") }
}
