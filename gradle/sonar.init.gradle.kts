initscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:7.3.0.8198")
    }
}

rootProject {
    apply<org.sonarqube.gradle.SonarQubePlugin>()

    configure<org.sonarqube.gradle.SonarExtension> {
        properties {
            property("sonar.projectKey", "iwakoshi_jandex-gradle-plugin")
            property("sonar.organization", "iwakoshi")
            property("sonar.host.url", "https://sonarcloud.io")
            property("sonar.coverage.jacoco.xmlReportPaths",
                "${project.layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml")
        }
    }
}

