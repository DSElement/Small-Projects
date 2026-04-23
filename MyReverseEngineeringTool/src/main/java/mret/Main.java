package mret;

import mret.analyzer.ClassAnalyzer;
import mret.loader.JarLoader;
import mret.model.ClassDiagram;
import mret.renderer.PlantUmlRenderer;
import mret.renderer.Renderer;
import mret.renderer.TextRenderer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class Main {

    private static final String USAGE = """
            Usage: java -jar mret.jar <jar-file> [options]

            Options:
              --filter <package-prefix>   Only include types matching the given package prefix.
                                          Can be specified multiple times to include several packages.
              --format <format>           Output format (default: text)
                                          Built-in: text, plantuml
                                          Custom:   fully-qualified class name implementing Renderer
              --output <file>             Write output to a file (UTF-8) instead of stdout
              --list-packages             List all packages in the JAR with class counts, then exit
              --help                      Show this help message

            Examples:
              java -jar mret.jar mylib.jar
              java -jar mret.jar mylib.jar --list-packages
              java -jar mret.jar mylib.jar --filter com.example.model
              java -jar mret.jar mylib.jar --filter com.example.model --filter com.example.api
              java -jar mret.jar mylib.jar --format plantuml --output diagram.puml
              java -cp mret.jar;myplugin.jar mret.Main mylib.jar --format com.acme.MermaidRenderer
            """;

    public static void main(String[] args) {
        if (args.length == 0 || contains(args, "--help")) {
            System.out.println(USAGE);
            System.exit(args.length == 0 ? 1 : 0);
        }

        String jarPathStr = args[0];
        List<String> packageFilters = new ArrayList<>();
        String format = "text";
        String outputFile = null;
        boolean listPackages = false;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--filter" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("Error: --filter requires a package prefix argument");
                        System.exit(1);
                    }
                    packageFilters.add(args[++i]);
                }
                case "--format" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("Error: --format requires 'text', 'plantuml', or a fully-qualified Renderer class name");
                        System.exit(1);
                    }
                    format = args[++i];
                }
                case "--output" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("Error: --output requires a file path");
                        System.exit(1);
                    }
                    outputFile = args[++i];
                }
                case "--list-packages" -> listPackages = true;
                default -> {
                    System.err.println("Error: Unknown option '" + args[i] + "'");
                    System.err.println(USAGE);
                    System.exit(1);
                }
            }
        }

        var jarPath = Path.of(jarPathStr);
        if (!Files.exists(jarPath)) {
            System.err.println("Error: File not found: " + jarPath);
            System.exit(1);
        }
        if (!jarPathStr.endsWith(".jar")) {
            System.err.println("Error: File does not appear to be a JAR: " + jarPath);
            System.exit(1);
        }

        try {
            if (listPackages) {
                var packages = JarLoader.listPackages(jarPath);
                System.out.println("Packages in " + jarPath.getFileName() + ":");
                int maxLen = packages.keySet().stream().mapToInt(String::length).max().orElse(0);
                for (var entry : packages.entrySet()) {
                    System.out.printf("  %-" + maxLen + "s  (%d class%s)%n",
                            entry.getKey(), entry.getValue(), entry.getValue() == 1 ? "" : "es");
                }
                System.exit(0);
            }

            System.err.println("Loading classes from: " + jarPath);
            var classes = JarLoader.loadClasses(jarPath, packageFilters);
            System.err.println("Loaded " + classes.size() + " class(es).");

            if (classes.isEmpty()) {
                System.err.println("No classes found" +
                        (!packageFilters.isEmpty() ? " matching filter(s): " + String.join(", ", packageFilters) : "") + ".");
                System.exit(0);
            }

            var jarClassNames = classes.stream().map(Class::getName).collect(Collectors.toSet());
            ClassDiagram diagram = ClassAnalyzer.analyze(classes, jarClassNames);

            Renderer renderer = switch (format) {
                case "text" -> new TextRenderer();
                case "plantuml" -> new PlantUmlRenderer();
                default -> loadRendererByName(format);
            };

            String output = renderer.render(diagram);

            if (outputFile != null) {
                Files.writeString(Path.of(outputFile), output, StandardCharsets.UTF_8);
                System.err.println("Output written to: " + outputFile);
            } else {
                System.out.println(output);
            }

        } catch (IOException e) {
            System.err.println("Error reading JAR file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static Renderer loadRendererByName(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!Renderer.class.isAssignableFrom(clazz)) {
                System.err.println("Error: " + className + " does not implement Renderer");
                System.exit(1);
            }
            return (Renderer) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            System.err.println("Error: Renderer class not found: " + className);
            System.exit(1);
        } catch (ReflectiveOperationException e) {
            System.err.println("Error: Could not instantiate renderer: " + e.getMessage());
            System.exit(1);
        }
        return null;
    }

    private static boolean contains(String[] args, String flag) {
        for (var arg : args)
            if (arg.equals(flag))
                return true;
        return false;
    }
}
