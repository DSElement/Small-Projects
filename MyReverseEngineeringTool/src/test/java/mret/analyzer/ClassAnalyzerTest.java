package mret.analyzer;

import mret.loader.JarLoader;
import mret.model.*;
import mret.testutil.TestJarBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ClassAnalyzerTest {

    @TempDir Path tempDir;
    private ClassDiagram diagram;

    @BeforeEach
    void setUp() throws IOException {
        Path testJar = TestJarBuilder.buildTestJar(tempDir);
        List<Class<?>> classes = JarLoader.loadClasses(testJar);
        Set<String> jarClassNames = classes.stream().map(Class::getName).collect(Collectors.toSet());
        diagram = ClassAnalyzer.analyze(classes, jarClassNames);
    }

    @Test void animalIsAbstractClass() {
        assertEquals(TypeKind.ABSTRACT_CLASS, findType("Animal").kind());
    }

    @Test void dogIsClass() {
        assertEquals(TypeKind.CLASS, findType("Dog").kind());
    }

    @Test void speakableIsInterface() {
        assertEquals(TypeKind.INTERFACE, findType("Speakable").kind());
    }

    @Test void myAnnotationIsAnnotation() {
        assertEquals(TypeKind.ANNOTATION, findType("MyAnnotation").kind());
    }

    @Test void animalFieldsExtracted() {
        var fields = findType("Animal").fields();
        var nameField = fields.stream().filter(f -> f.name().equals("name")).findFirst();
        var ageField = fields.stream().filter(f -> f.name().equals("age")).findFirst();

        assertAll(
                () -> assertTrue(nameField.isPresent()),
                () -> assertEquals(Visibility.PROTECTED, nameField.get().visibility()),
                () -> assertEquals("String", nameField.get().typeName()),
                () -> assertTrue(ageField.isPresent()),
                () -> assertEquals(Visibility.PRIVATE, ageField.get().visibility()),
                () -> assertEquals("int", ageField.get().typeName())
        );
    }

    @Test void dogStaticFinalField() {
        var species = findType("Dog").fields().stream().filter(f -> f.name().equals("SPECIES")).findFirst();
        assertTrue(species.isPresent());
        assertAll(
                () -> assertEquals(Visibility.PUBLIC, species.get().visibility()),
                () -> assertTrue(species.get().modifiers().contains("static")),
                () -> assertTrue(species.get().modifiers().contains("final")),
                () -> assertEquals("String", species.get().typeName())
        );
    }

    @Test void animalAbstractMethod() {
        var speak = findType("Animal").methods().stream().filter(m -> m.name().equals("speak")).findFirst();
        assertTrue(speak.isPresent());
        assertAll(
                () -> assertEquals("String", speak.get().returnTypeName()),
                () -> assertTrue(speak.get().modifiers().contains("abstract")),
                () -> assertEquals(Visibility.PUBLIC, speak.get().visibility())
        );
    }

    @Test void animalSleepMethod() {
        var sleep = findType("Animal").methods().stream().filter(m -> m.name().equals("sleep")).findFirst();
        assertTrue(sleep.isPresent());
        assertAll(
                () -> assertEquals(Visibility.PROTECTED, sleep.get().visibility()),
                () -> assertEquals("void", sleep.get().returnTypeName()),
                () -> assertEquals(1, sleep.get().parameters().size()),
                () -> assertEquals("int", sleep.get().parameters().get(0).typeName())
        );
    }

    @Test void animalConstructor() {
        var ctors = findType("Animal").constructors();
        assertEquals(1, ctors.size());
        assertAll(
                () -> assertEquals(Visibility.PUBLIC, ctors.get(0).visibility()),
                () -> assertEquals(2, ctors.get(0).parameters().size()),
                () -> assertEquals("String", ctors.get(0).parameters().get(0).typeName()),
                () -> assertEquals("int", ctors.get(0).parameters().get(1).typeName())
        );
    }

    @Test void dogConstructor() {
        assertEquals(3, findType("Dog").constructors().get(0).parameters().size());
    }

    @Test void genericContainerTypeParam() {
        var gc = findType("GenericContainer");
        assertEquals(1, gc.typeParameters().size());
        assertEquals("T", gc.typeParameters().get(0));
    }

    @Test void genericContainerGenericField() {
        var items = findType("GenericContainer").fields().stream().filter(f -> f.name().equals("items")).findFirst();
        assertTrue(items.isPresent());
        assertEquals("List<T>", items.get().typeName());
    }

    @Test void dogExtendsAnimal() {
        assertTrue(diagram.relationships().stream().anyMatch(
                r -> r.kind() == RelationshipKind.EXTENDS && r.fromType().endsWith("Dog") && r.toType().endsWith("Animal")));
    }

    @Test void dogImplementsSpeakable() {
        assertTrue(diagram.relationships().stream().anyMatch(
                r -> r.kind() == RelationshipKind.IMPLEMENTS && r.fromType().endsWith("Dog") && r.toType().endsWith("Speakable")));
    }

    @Test void dogUsesGenericContainer() {
        assertTrue(diagram.relationships().stream().anyMatch(
                r -> r.kind() == RelationshipKind.USES && r.fromType().endsWith("Dog") && r.toType().endsWith("GenericContainer")));
    }

    @Test void noJdkRelationships() {
        assertTrue(diagram.relationships().stream().map(Relationship::toType).noneMatch(t -> t.startsWith("java.")));
    }

    @Test void annotatedClassHasClassAnnotation() {
        assertTrue(findType("AnnotatedClass").annotations().contains("@MyAnnotation"));
    }

    @Test void annotatedFieldAnnotation() {
        var label = findType("AnnotatedClass").fields().stream().filter(f -> f.name().equals("label")).findFirst();
        assertTrue(label.isPresent());
        assertTrue(label.get().annotations().contains("@MyAnnotation"));
    }

    @Test void annotatedMethodAnnotation() {
        var method = findType("AnnotatedClass").methods().stream().filter(m -> m.name().equals("doSomething")).findFirst();
        assertTrue(method.isPresent());
        assertTrue(method.get().annotations().contains("@MyAnnotation"));
    }

    private TypeInfo findType(String simpleName) {
        return diagram.types().stream()
                .filter(t -> t.simpleName().equals(simpleName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Type not found: " + simpleName));
    }
}
