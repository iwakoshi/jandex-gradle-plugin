package io.github.iwakoshi.jandex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JandexPluginFunctionalTest {

    @TempDir
    File projectDir;

    private File buildFile;

    @BeforeEach
    void setup() {
        buildFile = new File(projectDir, "build.gradle");
    }

    private void writeString(File file, String string) throws IOException {
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(string);
        }
    }

    private void writeJavaSource(String className, String content) throws IOException {
        writeJavaSource(projectDir, className, content);
    }

    private void writeJavaSource(File baseDir, String className, String content) throws IOException {
        File sourceFile = new File(baseDir, "src/main/java/" + className + ".java");
        sourceFile.getParentFile().mkdirs();
        writeString(sourceFile, content);
    }

    private void setupBasicProject() throws IOException {
        writeString(buildFile, "plugins {\n"
            + "    id 'java'\n"
            + "    id 'io.github.iwakoshi.jandex'\n"
            + "}\n"
            + "repositories {\n"
            + "    mavenCentral()\n"
            + "}\n"
        );
        writeJavaSource("com/example/MyService",
            "package com.example;\n"
                + "public class MyService {\n"
                + "}\n"
        );
    }

    @Test
    void jandexTaskGeneratesIndex() throws IOException {
        setupBasicProject();

        BuildResult result = GradleRunner.create()
            .forwardOutput()
            .withProjectDir(projectDir)
            .withArguments("jandex")
            .withPluginClasspath()
            .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":jandex").getOutcome());
        File indexFile = new File(projectDir, "build/jandex/main/META-INF/jandex.idx");
        assertTrue(indexFile.exists());
    }

    @Test
    void jandexTaskIsUpToDateOnSecondRun() throws IOException {
        setupBasicProject();

        GradleRunner runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("jandex")
            .withPluginClasspath();

        runner.build();
        BuildResult result2 = runner.build();

        assertEquals(TaskOutcome.UP_TO_DATE, result2.task(":jandex").getOutcome());
    }

    @Test
    void jandexTaskRerunsWhenSourceChanges() throws IOException {
        setupBasicProject();

        GradleRunner runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("jandex")
            .withPluginClasspath();

        runner.build();

        // Change source
        writeJavaSource("com/example/MyService",
            "package com.example;\n"
                + "public class MyService {\n"
                + "    public void newMethod() {}\n"
                + "}\n"
        );

        BuildResult result2 = runner.build();
        assertEquals(TaskOutcome.SUCCESS, result2.task(":jandex").getOutcome());
    }

    @Test
    void buildTaskIncludesJandex() throws IOException {
        setupBasicProject();

        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build();

        assertNotNull(result.task(":jandex"));
        assertEquals(TaskOutcome.SUCCESS, result.task(":jandex").getOutcome());
    }

    @Test
    void configurationCacheIsCompatible() throws IOException {
        setupBasicProject();

        GradleRunner runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("jandex", "--configuration-cache")
            .withPluginClasspath();

        runner.build();
        BuildResult result2 = runner.build();

        assertEquals(TaskOutcome.UP_TO_DATE, result2.task(":jandex").getOutcome());
        assertTrue(result2.getOutput().contains("Reusing configuration cache"));
    }

    @Test
    void buildCacheIsSupported() throws IOException {
        setupBasicProject();

        // First build populates the cache
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("jandex", "--build-cache")
            .withPluginClasspath()
            .build();

        // Clean build outputs
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("clean", "--build-cache")
            .withPluginClasspath()
            .build();

        // Second build should restore from cache
        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("jandex", "--build-cache")
            .withPluginClasspath()
            .build();

        assertEquals(TaskOutcome.FROM_CACHE, result.task(":jandex").getOutcome());
    }

    @Test
    void customJandexVersionWorks() throws IOException {
        writeString(buildFile, "plugins {\n"
            + "    id 'java'\n"
            + "    id 'io.github.iwakoshi.jandex'\n"
            + "}\n"
            + "repositories {\n"
            + "    mavenCentral()\n"
            + "}\n"
            + "jandex {\n"
            + "    version = '3.0.0'\n"
            + "}\n"
        );
        writeJavaSource("com/example/MyService", "package com.example; public class MyService {}");

        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("jandex")
            .withPluginClasspath()
            .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":jandex").getOutcome());
    }

    @Test
    void emptyProjectProducesNoOutput() throws IOException {
        writeString(buildFile, "plugins {\n"
            + "    id 'java'\n"
            + "    id 'io.github.iwakoshi.jandex'\n"
            + "}\n"
            + "repositories {\n"
            + "    mavenCentral()\n"
            + "}\n"
        );

        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("jandex")
            .withPluginClasspath()
            .build();

        assertEquals(TaskOutcome.NO_SOURCE, result.task(":jandex").getOutcome());
    }

    @Test
    void worksWithJava8SourceCompatibility() throws IOException {
        writeString(buildFile, "plugins {\n"
            + "    id 'java'\n"
            + "    id 'io.github.iwakoshi.jandex'\n"
            + "}\n"
            + "repositories {\n"
            + "    mavenCentral()\n"
            + "}\n"
            + "java {\n"
            + "    sourceCompatibility ="
            + " JavaVersion.VERSION_1_8\n"
            + "    targetCompatibility ="
            + " JavaVersion.VERSION_1_8\n"
            + "}\n"
        );
        // Use Java 8 compatible code
        writeJavaSource("com/example/MyService",
            "package com.example;\n"
                + "import java.util.concurrent.Callable;\n"
                + "public class MyService implements"
                + " Callable<String> {\n"
                + "    @Override\n"
                + "    public String call() {"
                + " return \"hello\"; }\n"
                + "}\n"
        );

        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":jandex").getOutcome());

        // Verify the index is inside the JAR
        File jarFile = findJar(projectDir);
        assertTrue(jarContainsEntry(jarFile, "META-INF/jandex.idx"),
            "JAR should contain META-INF/jandex.idx even with Java 8 target");
    }

    @Test
    void jarContainsJandexIndex() throws IOException {
        setupBasicProject();

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("jar")
            .withPluginClasspath()
            .build();

        File jarFile = findJar(projectDir);
        assertNotNull(jarFile, "JAR file should have been created");
        assertTrue(jarFile.exists(), "JAR file should exist");
        assertTrue(jarContainsEntry(jarFile, "META-INF/jandex.idx"),
            "JAR should contain META-INF/jandex.idx for runtime discovery by CDI frameworks");
    }

    @Test
    void worksInMultiModuleProject() throws IOException {
        // Root project
        writeString(new File(projectDir, "settings.gradle"),
            "rootProject.name = 'multi-module-test'\n"
                + "include 'lib', 'app'\n"
        );
        writeString(new File(projectDir, "build.gradle"),
            "subprojects {\n"
                + "    apply plugin: 'java'\n"
                + "    repositories { mavenCentral() }\n"
                + "}\n"
        );

        // lib subproject
        File libDir = new File(projectDir, "lib");
        writeString(new File(libDir, "build.gradle"),
            "plugins {\n"
                + "    id 'io.github.iwakoshi.jandex'\n"
                + "}\n"
        );
        writeJavaSource(libDir, "com/example/lib/LibService",
            "package com.example.lib;\n"
                + "public class LibService {}\n"
        );

        // app subproject
        File appDir = new File(projectDir, "app");
        writeString(new File(appDir, "build.gradle"),
            "plugins {\n"
                + "    id 'io.github.iwakoshi.jandex'\n"
                + "}\n"
                + "dependencies {\n"
                + "    implementation project(':lib')\n"
                + "}\n"
        );
        writeJavaSource(appDir, "com/example/app/AppService",
            "package com.example.app;\n"
                + "public class AppService {}\n"
        );

        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":lib:jandex").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":app:jandex").getOutcome());

        // Both JARs should contain the index
        File libJar = findJar(libDir);
        File appJar = findJar(appDir);
        assertTrue(jarContainsEntry(libJar, "META-INF/jandex.idx"), "lib JAR should contain jandex index");
        assertTrue(jarContainsEntry(appJar, "META-INF/jandex.idx"), "app JAR should contain jandex index");
    }

    @Test
    void worksWithKotlinDsl() throws IOException {
        // Use .kts build file
        File ktsBuildFile = new File(projectDir, "build.gradle.kts");
        writeString(ktsBuildFile, "plugins {\n"
            + "    java\n"
            + "    id(\"io.github.iwakoshi.jandex\")\n"
            + "}\n"
            + "repositories {\n"
            + "    mavenCentral()\n"
            + "}\n"
            + "jandex {\n"
            + "    version.set(\"3.2.0\")\n"
            + "}\n"
        );
        writeJavaSource("com/example/MyService", "package com.example; public class MyService {}");

        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("jandex")
            .withPluginClasspath()
            .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":jandex").getOutcome());
    }

    @Test
    void worksWithJavaLibraryPlugin() throws IOException {
        writeString(buildFile, "plugins {\n"
            + "    id 'java-library'\n"
            + "    id 'io.github.iwakoshi.jandex'\n"
            + "}\n"
            + "repositories {\n"
            + "    mavenCentral()\n"
            + "}\n"
        );
        writeJavaSource("com/example/MyLibrary", "package com.example; public class MyLibrary {}");

        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":jandex").getOutcome());
        File jarFile = findJar(projectDir);
        assertTrue(jarContainsEntry(jarFile, "META-INF/jandex.idx"), "java-library JAR should contain jandex index");
    }

    @Test
    void worksWithKotlinJvmPlugin() throws IOException {
        writeString(new File(projectDir, "settings.gradle"),
            "pluginManagement {\n"
                + "    repositories {\n"
                + "        mavenCentral()\n"
                + "        gradlePluginPortal()\n"
                + "    }\n"
                + "}\n"
        );
        writeString(buildFile, "plugins {\n"
            + "    id 'org.jetbrains.kotlin.jvm'"
            + " version '1.9.25'\n"
            + "    id 'io.github.iwakoshi.jandex'\n"
            + "}\n"
            + "repositories {\n"
            + "    mavenCentral()\n"
            + "}\n"
        );
        // Write a Kotlin source file
        File kotlinSource = new File(projectDir, "src/main/kotlin/com/example/MyKotlinService.kt");
        kotlinSource.getParentFile().mkdirs();
        writeString(kotlinSource, "package com.example\n"
            + "class MyKotlinService {\n"
            + "    fun greet(): String = \"Hello\"\n"
            + "}\n"
        );

        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":jandex").getOutcome());
        File jarFile = findJar(projectDir);
        assertTrue(jarContainsEntry(jarFile, "META-INF/jandex.idx"), "Kotlin project JAR should contain jandex index");
    }

    @Test
    void worksWithApplicationPlugin() throws IOException {
        writeString(buildFile, "plugins {\n"
            + "    id 'application'\n"
            + "    id 'io.github.iwakoshi.jandex'\n"
            + "}\n"
            + "repositories {\n"
            + "    mavenCentral()\n"
            + "}\n"
            + "application {\n"
            + "    mainClass = 'com.example.Main'\n"
            + "}\n"
        );
        writeJavaSource("com/example/Main",
            "package com.example;\n"
                + "public class Main {\n"
                + "    public static void main"
                + "(String[] args) {}\n"
                + "}\n"
        );

        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("build")
            .withPluginClasspath()
            .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":jandex").getOutcome());
    }

    @Test
    void failsWithClearErrorWhenJavaPluginNotApplied() throws IOException {
        writeString(buildFile, "plugins {\n"
            + "    id 'io.github.iwakoshi.jandex'\n"
            + "}\n"
        );

        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("tasks", "--warning-mode", "all")
            .withPluginClasspath()
            .build();

        // Plugin should warn that jandex won't work without
        // java plugin
        assertTrue(result.getOutput().contains("'java' plugin is not applied"),
            "Should warn user that java plugin is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.0.2", "8.6", "8.10.2", "8.14.1"})
    void worksWithGradleVersion(String gradleVersion)
        throws IOException {
        setupBasicProject();

        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("jandex")
            .withPluginClasspath()
            .withGradleVersion(gradleVersion)
            .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":jandex").getOutcome());
        File indexFile = new File(projectDir, "build/jandex/main/META-INF/jandex.idx");
        assertTrue(indexFile.exists(), "Index should be generated with Gradle " + gradleVersion);
    }

    private File findJar(File projectBaseDir) {
        File libsDir = new File(projectBaseDir, "build/libs");
        if (!libsDir.exists()) {
            return null;
        }
        File[] jars = libsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        return (jars != null && jars.length > 0) ? jars[0] : null;
    }

    private boolean jarContainsEntry(File jarFile, String entryName) throws IOException {
        if (jarFile == null || !jarFile.exists()) {
            return false;
        }
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                if (entries.nextElement().getName()
                    .equals(entryName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
