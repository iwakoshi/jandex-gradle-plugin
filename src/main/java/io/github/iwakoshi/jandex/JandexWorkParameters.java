package io.github.iwakoshi.jandex;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkParameters;

/**
 * Parameters passed to {@link JandexWorkAction} via the Work API.
 * All fields are serializable Gradle managed types - safe for configuration
 * cache.
 */
public interface JandexWorkParameters extends WorkParameters {

    /**
     * The directories containing the {@code class} files to index.
     *
     * @return the classes directories file collection
     */
    ConfigurableFileCollection getClassesDirs();

    /**
     * The file path to the {@code META-INF/jandex.idx} file to be generated.
     *
     * @return the output file property
     */
    RegularFileProperty getOutputFile();

    /**
     * The version of the Jandex index format to use.
     *
     * @return the index version property
     */
    Property<Integer> getIndexVersion();
}
