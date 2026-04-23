package mret.loader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class JarLoader {

    private JarLoader() {}

    public static List<Class<?>> loadClasses(Path jarPath) throws IOException {
        return loadClasses(jarPath, List.of());
    }

    public static List<Class<?>> loadClasses(Path jarPath, String packageFilter) throws IOException {
        return loadClasses(jarPath, packageFilter == null ? List.of() : List.of(packageFilter));
    }

    public static List<Class<?>> loadClasses(Path jarPath, List<String> packageFilters) throws IOException {
        var classes = new ArrayList<Class<?>>();
        var jarUrl = jarPath.toUri().toURL();
        var classLoader = new URLClassLoader(new URL[]{jarUrl}, ClassLoader.getSystemClassLoader());

        try (var jarFile = new JarFile(jarPath.toFile())) {
            var entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (!name.endsWith(".class") || entry.isDirectory()) continue;

                // com/example/Foo.class → com.example.Foo
                String className = name.substring(0, name.length() - 6).replace('/', '.');

                if (className.endsWith("module-info") || className.endsWith("package-info")) continue;
                if (!packageFilters.isEmpty() && packageFilters.stream().noneMatch(className::startsWith)) continue;

                try {
                    Class<?> clazz = classLoader.loadClass(className);
                    if (clazz.isAnonymousClass() || clazz.isLocalClass() || clazz.isSynthetic()) continue;
                    classes.add(clazz);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    System.err.println("[WARN] Could not load class: " + className + " — " + e.getMessage());
                } catch (Throwable e) {
                    System.err.println("[WARN] Unexpected error loading class: " + className + " — " + e.getMessage());
                }
            }
        }

        return classes;
    }

    /**
     * Lists all unique package names found in the JAR, along with the number of classes in each.
     * Does not load any classes — only inspects entry names.
     */
    public static Map<String, Integer> listPackages(Path jarPath) throws IOException {
        var packages = new TreeMap<String, Integer>();

        try (var jarFile = new JarFile(jarPath.toFile())) {
            var entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (!name.endsWith(".class") || entry.isDirectory()) continue;

                String className = name.substring(0, name.length() - 6).replace('/', '.');
                if (className.endsWith("module-info") || className.endsWith("package-info")) continue;

                int lastDot = className.lastIndexOf('.');
                String pkg = lastDot >= 0 ? className.substring(0, lastDot) : "(default package)";
                packages.merge(pkg, 1, Integer::sum);
            }
        }

        return packages;
    }
}
