import org.gradle.plugin.compatibility.compatibility
plugins {
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.plugin.publish)
    jacoco
    checkstyle
}

group = "io.github.iwakoshi"
version = project.findProperty("pluginVersion") ?: "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jandex)
}

java {
    withSourcesJar()
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

tasks.jacocoTestReport {
    dependsOn(tasks.test, testing.suites.named("functionalTest"))
    executionData.setFrom(fileTree(layout.buildDirectory) { include("jacoco/*.exec") })
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit.get())
        }

        val functionalTest by registering(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit.get())
            dependencies {
                implementation(project())
                implementation(libs.junit.params)
            }

            targets {
                all {
                    testTask.configure { shouldRunAfter(test) }
                }
            }
        }
    }
}

gradlePlugin {
    website.set("https://github.com/iwakoshi/jandex-gradle-plugin")
    vcsUrl.set("https://github.com/iwakoshi/jandex-gradle-plugin")

    val jandex by plugins.creating {
        id = "io.github.iwakoshi.jandex"
        implementationClass = "io.github.iwakoshi.jandex.JandexPlugin"
        displayName = "Jandex Gradle Plugin"
        description = "Generates Jandex CDI bean indexes (META-INF/jandex.idx) for your Java projects"
        tags.set(listOf("jandex", "cdi", "index", "quarkus", "smallrye", "bean-discovery"))
        compatibility {
            features {
                configurationCache = true
            }
        }
    }
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

tasks.named<Task>("check") {
    dependsOn(testing.suites.named("functionalTest"))
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/iwakoshi/jandex-gradle-plugin")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
