package io.github.iwakoshi.jandex;

import org.gradle.api.provider.Property;

/**
 * Extension exposed as {@code jandex {}} DSL block in {@code build.gradle}.
 *
 * <pre>
 *  jandex {
 *      version = "3.5.3"
 *      processTestClasses = false
 *      indexVersion = 6
 *  }
 * </pre>
 */
public abstract class JandexExtension {

    /**
     * The version of {@code io.smallrye:jandex} to use for indexing.
     * Default value is {@code 3.5.3}.
     *
     * @return the jandex version property
     */
    public abstract Property<String> getVersion();

    /**
     * Whether to process test classes.
     * Default value is {@code false}.
     *
     * @return the process test classes property
     */
    public abstract Property<Boolean> getProcessTestClasses();

    /**
     * The Jandex index format version to write. When unset, the latest version
     * supported by the used Jandex version is used. Set this to an older
     * version (e.g. {@code 6}) for compatibility with readers that don't
     * support the latest format.
     * Value must be &gt;= 6.
     *
     * @return the index version property
     */
    public abstract Property<Integer> getIndexVersion();
}
