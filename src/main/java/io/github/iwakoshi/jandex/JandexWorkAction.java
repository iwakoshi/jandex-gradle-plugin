package io.github.iwakoshi.jandex;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.TreeMap;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.workers.WorkAction;
import org.jboss.jandex.ClassSummary;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.jetbrains.annotations.NotNull;

/**
 * Worker action that runs in a classloader-isolated environment to avoid
 * dependency conflicts with the user project.
 *
 * <p>The {@code io.smallrye:jandex} dependency is loaded from the isolated
 * classpath provided by the Gradle Worker API, so the plugin Jar itself does
 * not need to bundle it. This enables consumers to choose any compatible
 * Jandex version at configuration time.
 *
 * <p>Class files are collected and sorted by URI before indexing, ensuring
 * reproducible indexes across different operating systems and file systems
 * (requires Jandex &ge; 3.5.0).
 */
public abstract class JandexWorkAction implements WorkAction<JandexWorkParameters> {

    private static final Logger LOGGER = Logging.getLogger(JandexWorkAction.class);

    @Override
    public void execute() {
        JandexWorkParameters params = getParameters();
        File outputFile = params.getOutputFile().getAsFile().get();

        try {
            Indexer indexer = new Indexer();

            Map<URI, File> classFiles = new TreeMap<>();
            for (File classesDir : params.getClassesDirs()) {
                if (!classesDir.exists()) {
                    continue;
                }
                collectClassFiles(classesDir.toPath(), classFiles);
            }

            if (classFiles.isEmpty()) {
                return;
            }

            int totalAnnotations = 0;
            for (Map.Entry<URI, File> entry : classFiles.entrySet()) {
                try (FileInputStream fis = new FileInputStream(entry.getValue())) {
                    ClassSummary summary = indexer.indexWithSummary(fis);
                    if (summary != null) {
                        LOGGER.info("Indexed {} ({} annotations)", summary.name(), summary.annotationsCount());
                        totalAnnotations += summary.annotationsCount();
                    }
                }
            }

            LOGGER.lifecycle("Jandex: indexed {} classes ({} annotations) -> {}",
                classFiles.size(), totalAnnotations, outputFile);

            Index index = indexer.complete();
            writeIndex(index, outputFile, params.getIndexVersion().getOrNull());
        } catch (Exception ex) {
            throw new GradleException("Failed to generate Jandex index", ex);
        }
    }

    private void collectClassFiles(Path root, Map<URI, File> classFiles) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public @NotNull FileVisitResult visitFile(
                    @NotNull Path file,
                    @NotNull BasicFileAttributes attrs) {
                if (file.toString().endsWith(".class")) {
                    classFiles.put(file.toUri(), file.toFile());
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void writeIndex(Index index, File outputFile, Integer indexVersion) throws IOException {
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + parentDir);
        }

        try (OutputStream out = Files.newOutputStream(outputFile.toPath())) {
            IndexWriter writer = new IndexWriter(out);
            if (indexVersion != null && indexVersion > 0) {
                writer.write(index, indexVersion);
            } else {
                writer.write(index);
            }
        }
    }
}
