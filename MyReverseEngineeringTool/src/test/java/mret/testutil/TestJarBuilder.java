package mret.testutil;

import java.io.*;
import java.nio.file.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public final class TestJarBuilder {

    private static final String FIXTURE_PACKAGE = "mret/testfixtures";

    private static final String[] FIXTURE_CLASSES = {
            "Animal", "Speakable", "Dog", "MyAnnotation", "AnnotatedClass", "GenericContainer"
    };

    private TestJarBuilder() {}

    public static Path buildTestJar(Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        Path jarPath = targetDir.resolve("test-fixtures.jar");

        try (var jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            for (String className : FIXTURE_CLASSES) {
                String resourcePath = FIXTURE_PACKAGE + "/" + className + ".class";

                try (var is = TestJarBuilder.class.getClassLoader().getResourceAsStream(resourcePath)) {
                    if (is == null) throw new IOException("Could not find class resource: " + resourcePath);
                    jos.putNextEntry(new JarEntry(resourcePath));
                    is.transferTo(jos);
                    jos.closeEntry();
                }
            }
        }

        return jarPath;
    }
}
