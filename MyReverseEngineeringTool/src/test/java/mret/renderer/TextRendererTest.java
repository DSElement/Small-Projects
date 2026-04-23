package mret.renderer;

import mret.analyzer.ClassAnalyzer;
import mret.loader.JarLoader;
import mret.model.ClassDiagram;
import mret.testutil.TestJarBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TextRendererTest {

    @TempDir Path tempDir;
    private String output;

    @BeforeEach
    void setUp() throws IOException {
        Path testJar = TestJarBuilder.buildTestJar(tempDir);
        List<Class<?>> classes = JarLoader.loadClasses(testJar);
        Set<String> jarClassNames = classes.stream().map(Class::getName).collect(Collectors.toSet());
        ClassDiagram diagram = ClassAnalyzer.analyze(classes, jarClassNames);
        output = new TextRenderer().render(diagram);
    }

    @Test void containsHeader()              { assertTrue(output.contains("CLASS DIAGRAM")); }
    @Test void containsFieldsSection()       { assertTrue(output.contains("Fields:")); }
    @Test void containsMethodsSection()      { assertTrue(output.contains("Methods:")); }
    @Test void containsConstructorsSection() { assertTrue(output.contains("Constructors:")); }
    @Test void containsRelationshipsSection(){ assertTrue(output.contains("RELATIONSHIPS")); }

    @Test void publicSymbol()    { assertTrue(output.contains("+ getBreed")); }
    @Test void privateSymbol()   { assertTrue(output.contains("- age")); }
    @Test void protectedSymbol() { assertTrue(output.contains("# name")); }

    @Test void interfaceStereotype()  { assertTrue(output.contains("«interface»")); }
    @Test void abstractStereotype()   { assertTrue(output.contains("«abstract»")); }
    @Test void annotationStereotype() { assertTrue(output.contains("«annotation»")); }

    @Test void staticFinalModifiers()   { assertTrue(output.contains("{static, final}")); }
    @Test void abstractMethodModifier() { assertTrue(output.contains("{abstract}")); }

    @Test void extendsArrow()     { assertTrue(output.contains("──▶")); }
    @Test void implementsArrow()  { assertTrue(output.contains("--▷")); }

    @Test void relationshipLabels() {
        assertAll(
                () -> assertTrue(output.contains("[extends]")),
                () -> assertTrue(output.contains("[implements]"))
        );
    }

    @Test void plantUmlBasicStructure() throws IOException {
        Path testJar = TestJarBuilder.buildTestJar(tempDir.resolve("plantuml-test"));
        List<Class<?>> classes = JarLoader.loadClasses(testJar);
        Set<String> jarClassNames = classes.stream().map(Class::getName).collect(Collectors.toSet());
        String puml = new PlantUmlRenderer().render(ClassAnalyzer.analyze(classes, jarClassNames));

        assertAll(
                () -> assertTrue(puml.startsWith("@startuml")),
                () -> assertTrue(puml.trim().endsWith("@enduml")),
                () -> assertTrue(puml.contains("class")),
                () -> assertTrue(puml.contains("interface")),
                () -> assertTrue(puml.contains("--|>")),
                () -> assertTrue(puml.contains("..|>"))
        );
    }
}
