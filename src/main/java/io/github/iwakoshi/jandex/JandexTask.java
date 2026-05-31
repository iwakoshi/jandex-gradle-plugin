package io.github.iwakoshi.jandex;

import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkerExecutor;

/**
 * Gradle task that generates a Jandex index for a given set of class
 * directories. Uses the Gradle Worker API with classloader isolation.
 */
@CacheableTask
public abstract class JandexTask extends DefaultTask {

    /**
     * The directories containing the class files to index.
     * When empty, the task will not run.
     *
     * @return the classes directories file collection
     */
    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getClassesDirs();

    /**
     * The classpath containing the {@code io.smallrye:jandex} library.
     * Resolved from a detached configuration at configuration time.
     *
     * @return the jandex classpath file collection
     */
    @Classpath
    public abstract ConfigurableFileCollection getJandexClasspath();

    /**
     * The file path to the Jandex index file to be generated.
     *
     * @return the output file property
     */
    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    /**
     * The version of the Jandex index format to use.
     * When unset, the latest version supported by the used Jandex version
     * is used. Set this to an older version (e.g. {@code 6}) for
     * compatibility with readers that don't support the latest format.
     * Value must be &gt;= 6.
     *
     * @return the index version property
     */
    @Input
    @Optional
    public abstract Property<Integer> getIndexVersion();

    @Inject
    public abstract WorkerExecutor getWorkerExecutor();

    /**
     * Executes the Jandex indexing via the Worker API.
     */
    @TaskAction
    public void generateIndex() {
        getWorkerExecutor()
            .classLoaderIsolation(spec ->
                spec.getClasspath().from(getJandexClasspath()))
            .submit(JandexWorkAction.class, params -> {
                params.getClassesDirs().from(getClassesDirs());
                params.getOutputFile().set(getOutputFile());
                params.getIndexVersion().set(getIndexVersion());
            });
    }
}
