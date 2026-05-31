package io.github.iwakoshi.jandex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JandexPluginTest {

    private Project project;

    @BeforeEach
    void setup() {
        project = ProjectBuilder.builder().build();
        project.getPlugins().apply("java");
        project.getPlugins().apply("io.github.iwakoshi.jandex");
    }

    @Test
    void extensionIsRegistered() {
        JandexExtension extension = project.getExtensions().findByType(JandexExtension.class);
        assertNotNull(extension, "JandexExtension should be registered");
    }

    @Test
    void extensionHasDefaultVersion() {
        JandexExtension extension = project.getExtensions().findByType(JandexExtension.class);
        assertEquals(JandexPlugin.DEFAULT_JANDEX_VERSION, extension.getVersion().get());
    }

    @Test
    void extensionHasDefaultProcessTestClasses() {
        JandexExtension extension = project.getExtensions().findByType(JandexExtension.class);
        assertEquals(false, extension.getProcessTestClasses().get());
    }

    @Test
    void jandexMainTaskIsRegistered() {
        assertTrue(project.getTasks().getNames().contains("jandex"), "Task 'jandex' should be registered");
    }

    @Test
    void jandexMainTaskIsOfTypeJandexTask() {
        assertInstanceOf(JandexTask.class, project.getTasks().getByName("jandex"),
            "Task 'jandex' should be of type JandexTask");
    }

    @Test
    void jandexClasspathConfigurationExists() {
        assertNotNull(project.getConfigurations().findByName("jandexClasspath"),
            "Configuration 'jandexClasspath' should be registered");
    }

    @Test
    void jandexClasspathIsNotConsumable() {
        assertFalse(project.getConfigurations().findByName("jandexClasspath").isCanBeConsumed(),
            "Configuration 'jandexClasspath' should not be consumable");
    }

    @Test
    void jandexClasspathIsResolved() {
        assertTrue(project.getConfigurations().findByName("jandexClasspath").isCanBeResolved(),
            "Configuration 'jandexClasspath' should be resolved");
    }

    @Test
    void jandexClasspathIsVisible() {
        assertFalse(project.getConfigurations().findByName("jandexClasspath").isVisible(),
            "Configuration 'jandexClasspath' should be visible");
    }

    @Test
    void jandexOutputFileIsConfigured() {
        JandexTask task = project.getTasks().named("jandex", JandexTask.class).get();
        String outputPath = task.getOutputFile().get().getAsFile().getAbsolutePath();
        assertTrue(outputPath.contains("jandex/main") && outputPath.endsWith("META-INF/jandex.idx"),
            "Jandex index file should be generated in jandex/main/META-INF/jandex.idx, was: " + outputPath);
    }

    @Test
    void pluginDoesNothingWhenJavaPluginIsNotApplied() {
        Project pr = ProjectBuilder.builder().build();
        pr.getPlugins().apply(JandexPlugin.class);
        assertFalse(pr.getTasks().getNames().contains("jandex"),
            "Task 'jandex' should not be registered without java plugin");
    }

    @Test
    void pluginAppliesWhenJavaPluginIsAppliedAfterJandexPlugin() {
        Project pr = ProjectBuilder.builder().build();
        pr.getPlugins().apply(JandexPlugin.class);
        pr.getPlugins().apply("java");
        assertTrue(pr.getTasks().getNames().contains("jandex"), "Task 'jandex' should be registered after java plugin");
    }

    @Test
    void customJandexVersionIsApplied() {
        JandexExtension ext = project.getExtensions().findByType(JandexExtension.class);
        ext.getVersion().set("3.0.0");
        assertEquals("3.0.0", ext.getVersion().get());
    }
}
