package mret.loader;

import mret.testutil.TestJarBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class JarLoaderTest {

    @TempDir Path tempDir;
    private Path testJar;

    @BeforeEach
    void setUp() throws IOException { testJar = TestJarBuilder.buildTestJar(tempDir); }

    @Test void loadsAllExpectedClasses() throws IOException {
        var classNames = JarLoader.loadClasses(testJar).stream().map(Class::getSimpleName).collect(Collectors.toSet());
        assertAll(
                () -> assertTrue(classNames.contains("Animal")),
                () -> assertTrue(classNames.contains("Dog")),
                () -> assertTrue(classNames.contains("Speakable")),
                () -> assertTrue(classNames.contains("MyAnnotation")),
                () -> assertTrue(classNames.contains("AnnotatedClass")),
                () -> assertTrue(classNames.contains("GenericContainer"))
        );
    }

    @Test void noDuplicateClasses() throws IOException {
        var names = JarLoader.loadClasses(testJar).stream().map(Class::getName).toList();
        assertEquals(Set.copyOf(names).size(), names.size());
    }

    @Test void loadsCorrectCount() throws IOException {
        assertEquals(6, JarLoader.loadClasses(testJar).size());
    }

    @Test void packageFilterExcludesNonMatching() throws IOException {
        assertTrue(JarLoader.loadClasses(testJar, "com.nonexistent").isEmpty());
    }

    @Test void packageFilterMatchesCorrectPrefix() throws IOException {
        assertEquals(6, JarLoader.loadClasses(testJar, "mret.testfixtures").size());
    }

    @Test void throwsForMissingJar() {
        assertThrows(IOException.class, () -> JarLoader.loadClasses(tempDir.resolve("nope.jar")));
    }
}
