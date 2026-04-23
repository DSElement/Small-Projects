# MRET — My Reverse Engineering Tool

A command-line Java 21 tool that introspects JAR files using reflection and produces structured class diagrams in text or PlantUML format.

Given any `.jar` file, MRET loads its classes at runtime, analyzes their fields, methods, constructors, annotations, generics, and relationships (inheritance, interface implementation, usage), and renders the result as a human-readable diagram.

## Requirements

- **Java 21** or later
- **Maven 3.8+** for building

## Building

```bash
mvn package
```

This compiles the source, runs the tests, and produces `target/my-reverse-engineering-tool-1.0.jar`.

To skip tests:

```bash
mvn package -DskipTests
```

## Usage

```
java -jar target/my-reverse-engineering-tool-1.0.jar <jar-file> [options]
```

### Options

| Option | Description |
|---|---|
| `--filter <package-prefix>` | Only include classes whose fully-qualified name starts with the given prefix. Can be specified multiple times. |
| `--format <format>` | Output format: `text` (default), `plantuml`, or a fully-qualified custom `Renderer` class name |
| `--output <file>` | Write output to a file with UTF-8 encoding instead of printing to stdout |
| `--list-packages` | List all packages in the JAR with class counts, then exit. Useful for finding the right `--filter` value. |
| `--help` | Show usage information |

### Examples

Analyze a JAR and print a text diagram:

```bash
java -jar mret.jar mylib.jar
```

List all packages to find out what's inside (useful for fat JARs with bundled dependencies):

```bash
java -jar mret.jar mylib.jar --list-packages
```

Filter to a specific package:

```bash
java -jar mret.jar mylib.jar --filter com.example.model
```

Filter to multiple packages:

```bash
java -jar mret.jar mylib.jar --filter com.example.model --filter com.example.api
```

Generate PlantUML output and write to a file:

```bash
java -jar mret.jar mylib.jar --format plantuml --output diagram.puml
```

## Architecture

The tool follows a linear pipeline:

```
JAR file ──► JarLoader ──► List<Class<?>> ──► ClassAnalyzer ──► ClassDiagram ──► Renderer ──► String
```

### Packages

| Package | Responsibility |
|---|---|
| `mret` | Entry point (`Main.java`) — CLI parsing, orchestration |
| `mret.loader` | `JarLoader` — opens JARs, loads `.class` files via `URLClassLoader`, applies package filtering |
| `mret.analyzer` | `ClassAnalyzer` — uses Java reflection to inspect loaded classes and build the model |
| `mret.model` | Data model — immutable records (`TypeInfo`, `FieldInfo`, `MethodInfo`, `ConstructorInfo`, `Relationship`, `ClassDiagram`, etc.) |
| `mret.renderer` | Output renderers — `TextRenderer`, `PlantUmlRenderer`, and the `Renderer` interface |
| `mret.util` | Shared utilities — `ModifierHelper` (extracts visibility and keyword modifiers), `TypeNameFormatter` (produces clean type names from reflection `Type` objects) |

### What gets analyzed

For each class in the JAR, the analyzer extracts:

- **Type kind** — class, abstract class, interface, enum, or annotation
- **Visibility** — public, protected, package-private, private
- **Modifiers** — static, final, abstract, synchronized, etc.
- **Generic type parameters** — including bounds (`T extends Comparable`)
- **Fields** — name, type, visibility, modifiers, annotations
- **Methods** — name, return type, parameters (with names if compiled with `-parameters`), thrown exceptions, visibility, modifiers, annotations
- **Constructors** — parameters, visibility, annotations
- **Relationships** — extends, implements, and uses (inferred from field types and method signatures)

Synthetic, anonymous, and local classes are skipped, as are compiler-generated synthetic members (bridge methods, `this$0` fields, etc.). Only declared members are shown — inherited members are omitted since the inheritance relationship is captured separately.

## Custom Renderers (Plugin System)

The `Renderer` interface is open for extension. Anyone can write their own renderer and use it without modifying MRET's source code.

### The interface

```java
package mret.renderer;

import mret.model.ClassDiagram;

public interface Renderer {
    String render(ClassDiagram diagram);
}
```

### Writing a custom renderer

Create a class that implements `Renderer` with a public no-argument constructor:

```java
package com.acme;

import mret.model.ClassDiagram;
import mret.renderer.Renderer;

public class MermaidRenderer implements Renderer {
    @Override
    public String render(ClassDiagram diagram) {
        var sb = new StringBuilder();
        sb.append("classDiagram\n");
        for (var type : diagram.types()) {
            sb.append("  class ").append(type.simpleName()).append("\n");
        }
        // ... build the rest of the Mermaid syntax
        return sb.toString();
    }
}
```

Compile it into a JAR (e.g., `myplugin.jar`), making sure `mret.jar` is on the compile classpath so the compiler can see the `Renderer` interface and the `model` records.

### Using a custom renderer

Put both JARs on the classpath and pass the fully-qualified class name to `--format`:

```bash
java -cp mret.jar;myplugin.jar mret.Main mylib.jar --format com.acme.MermaidRenderer
```

> On Linux/macOS, use `:` instead of `;` as the classpath separator.

Under the hood, MRET uses Java reflection (`Class.forName`, `isAssignableFrom`, `getDeclaredConstructor().newInstance()`) to discover, validate, and instantiate the renderer at runtime — no compile-time dependency on the plugin is needed.

## Output Formats

### Text

The default format. Produces a structured ASCII diagram:

```
╔════════════════════════════════════════════════════════════╗
║                   CLASS DIAGRAM                          ║
╚════════════════════════════════════════════════════════════╝

════════════════════════════════════════════════════════════
«interface» com.example.Speakable
════════════════════════════════════════════════════════════
Methods:
  + speak() : String
════════════════════════════════════════════════════════════

════════════════════════════════════════════════════════════
RELATIONSHIPS
════════════════════════════════════════════════════════════
  Dog ──▶ Animal  [extends]
  Dog --▷ Speakable  [implements]
════════════════════════════════════════════════════════════
```

### PlantUML

Produces a `.puml` file that can be rendered with [PlantUML](https://plantuml.online/) into an image:

```bash
java -jar mret.jar mylib.jar --format plantuml --output diagram.puml
java -jar plantuml.jar diagram.puml
```

## Project Structure

```
src/
├── main/java/mret/
│   ├── Main.java                  # CLI entry point
│   ├── loader/
│   │   └── JarLoader.java         # JAR loading via URLClassLoader
│   ├── analyzer/
│   │   └── ClassAnalyzer.java     # Reflection-based class analysis
│   ├── model/
│   │   ├── ClassDiagram.java      # Top-level output record
│   │   ├── TypeInfo.java          # Full type descriptor
│   │   ├── FieldInfo.java         # Field descriptor
│   │   ├── MethodInfo.java        # Method descriptor
│   │   ├── ConstructorInfo.java   # Constructor descriptor
│   │   ├── ParameterInfo.java     # Parameter descriptor
│   │   ├── Relationship.java      # Type relationship (from → to)
│   │   ├── RelationshipKind.java  # extends / implements / uses
│   │   ├── TypeKind.java          # class / interface / enum / etc.
│   │   └── Visibility.java        # public / protected / private / package
│   ├── renderer/
│   │   ├── Renderer.java          # Renderer interface (open for plugins)
│   │   ├── TextRenderer.java      # ASCII text output
│   │   └── PlantUmlRenderer.java  # PlantUML output
│   └── util/
│       ├── ModifierHelper.java    # Modifier bitmask → visibility + keywords
│       └── TypeNameFormatter.java # Type → clean readable name
└── test/java/mret/
    ├── analyzer/                  # ClassAnalyzer tests
    ├── loader/                    # JarLoader tests
    ├── renderer/                  # Renderer tests
    ├── testfixtures/              # Sample classes used in tests
    └── testutil/                  # Test helper utilities
```

## License

This project is not currently licensed. All rights reserved.
