package io.github.iwakoshi.jandex;

import java.io.File;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

/**
 * Gradle plugin to generate Jandex CDI bean indexes
 * ({@code META-INF/jandex.idx}).
 *
 * <p>Usage:
 *
 * <pre>
 * plugins {
 *      id("io.github.iwakoshi.jandex")
 * }
 * </pre>
 *
 * <p>The plugin reacts to the {@link JavaPlugin} being applied and
 * automatically registers a {@code jandex} task for the {@code main} source
 * set. If {@link JandexExtension#getProcessTestClasses()} is enabled, it also
 * registers a {@code jandexTest} task for the {@code test} source set.
 *
 * <p>The {@code io.smallrye:jandex} library version is configurable via the
 * {@link JandexExtension#getVersion()} property and defaults to
 * {@code 3.6.0}. The library is resolved into a detached configuration and
 * loaded in an isolated classloader via the Gradle Worker API, keeping the
 * project's dependency graph clean.
 *
 * @see JandexTask
 * @see JandexExtension
 */
public class JandexPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "jandex";
    private static final String MAIN_TASK_NAME = "jandex";
    private static final String TEST_TASK_NAME = "jandexTest";

    static final String DEFAULT_JANDEX_VERSION = "3.6.0";

    private static final String JANDEX_CLASSPATH_CONFIG = "jandexClasspath";
    private static final String JANDEX_DEPENDENCY = "io.smallrye:jandex:";

    /**
     * {@inheritDoc}
     */
    public void apply(Project project) {
        JandexExtension extension = project.getExtensions().create(EXTENSION_NAME, JandexExtension.class);
        extension.getVersion().convention(DEFAULT_JANDEX_VERSION);
        extension.getProcessTestClasses().convention(false);

        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> configureForJava(project, extension));

        project.afterEvaluate(p -> {
            if (!p.getPlugins().hasPlugin(JavaPlugin.class)) {
                project.getLogger().warn("Jandex plugin applied to project '{}' but the 'java' "
                        + "plugin is not applied. The jandex task will "
                        + "not be registered. Apply the 'java' plugin "
                        + "to enable Jandex indexing.",
                    project.getName());
            }
        });
    }

    private void configureForJava(Project project, JandexExtension extension) {
        Configuration jandexClasspath = project.getConfigurations()
            .create(JANDEX_CLASSPATH_CONFIG, conf -> {
                conf.setDescription("Classpath for the Jandex indexer Worker API execution");
                conf.setCanBeConsumed(false);
                conf.setCanBeResolved(true);
                conf.setVisible(false);
                conf.defaultDependencies(deps -> deps.add(
                    project.getDependencies().create(JANDEX_DEPENDENCY + extension.getVersion().get())));
            });

        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);

        TaskProvider<JandexTask> jandexMain = registerJandexTask(project,
            MAIN_TASK_NAME,
            sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME),
            jandexClasspath, extension);

        project.getTasks().named("jar", Jar.class, jar -> {
            jar.dependsOn(jandexMain);
            jar.from(jandexMain.map(task -> task.getOutputFile().get()
                .getAsFile().getParentFile().getParentFile()));
        });

        wireTaskOrdering(project, jandexMain);

        project.afterEvaluate(p -> {
            if (Boolean.TRUE.equals(extension.getProcessTestClasses().getOrNull())) {
                TaskProvider<JandexTask> jandexTest = registerJandexTask(
                    project, TEST_TASK_NAME,
                    sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME),
                    jandexClasspath, extension);
                p.getTasks().named(JavaPlugin.TEST_TASK_NAME, task -> task.dependsOn(jandexTest));
                wireTaskOrdering(project, jandexTest);
            }
        });
    }

    private TaskProvider<JandexTask> registerJandexTask(Project project,
                                                        String taskName,
                                                        SourceSet sourceSet,
                                                        Configuration jandexClasspath,
                                                        JandexExtension extension) {
        return project.getTasks().register(taskName, JandexTask.class,
            task -> {
                task.setDescription("Generate Jandex index for " + sourceSet.getName() + " source set");
                task.setGroup("indexing");
                task.getClassesDirs().from(sourceSet.getOutput().getClassesDirs());
                task.getJandexClasspath().from(jandexClasspath);

                File outputDir = project.getLayout()
                    .getBuildDirectory()
                    .dir("jandex/" + sourceSet.getName())
                    .get().getAsFile();
                task.getOutputFile().set(new File(outputDir, "META-INF/jandex.idx"));
                task.getIndexVersion().set(extension.getIndexVersion());

                task.dependsOn(sourceSet.getCompileJavaTaskName());
            });
    }

    private void wireTaskOrdering(Project project, TaskProvider<JandexTask> jandexTask) {
        project.getTasks().configureEach(task -> {
            String name = task.getName();
            if (name.equals("javadoc")
                || name.startsWith("checkstyle")
                || name.startsWith("spotbugs")
                || name.startsWith("pmd")
                || name.startsWith("errorprone")) {
                task.mustRunAfter(jandexTask);
            }
        });
    }
}
