package mret.analyzer;

import mret.model.*;
import mret.util.ModifierHelper;
import mret.util.TypeNameFormatter;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public final class ClassAnalyzer {

    private ClassAnalyzer() {
    }

    public static ClassDiagram analyze(List<Class<?>> classes, Set<String> jarClassNames) {
        var types = new ArrayList<TypeInfo>();
        var relationships = new ArrayList<Relationship>();

        for (var clazz : classes) {
            try {
                types.add(analyzeType(clazz));
                collectRelationships(clazz, jarClassNames, relationships);
            } catch (Throwable e) {
                System.err.println("[WARN] Could not analyze class: " + clazz.getName() + " — " + e.getMessage());
            }
        }

        types.sort(Comparator.comparing(TypeInfo::fullyQualifiedName));

        var uniqueRelationships = relationships.stream()
                .distinct()
                .sorted(Comparator.comparing(Relationship::fromType)
                        .thenComparing(Relationship::kind)
                        .thenComparing(Relationship::toType))
                .toList();

        return new ClassDiagram(types, uniqueRelationships);
    }

    private static TypeInfo analyzeType(Class<?> clazz) {
        int mod = clazz.getModifiers();

        return new TypeInfo(
                determineTypeKind(clazz),
                ModifierHelper.visibilityFrom(mod),
                ModifierHelper.extractModifiers(mod),
                clazz.getPackageName(),
                clazz.getName(),
                clazz.getSimpleName(),
                extractTypeParameters(clazz),
                extractSuperclass(clazz),
                extractInterfaces(clazz),
                extractFields(clazz),
                extractMethods(clazz),
                extractConstructors(clazz),
                extractAnnotations(clazz.getAnnotations()));
    }

    private static TypeKind determineTypeKind(Class<?> clazz) {
        if (clazz.isAnnotation())
            return TypeKind.ANNOTATION;
        if (clazz.isEnum())
            return TypeKind.ENUM;
        if (clazz.isInterface())
            return TypeKind.INTERFACE;
        if (Modifier.isAbstract(clazz.getModifiers()))
            return TypeKind.ABSTRACT_CLASS;
        return TypeKind.CLASS;
    }

    private static List<String> extractTypeParameters(Class<?> clazz) {
        TypeVariable<?>[] params = clazz.getTypeParameters();
        if (params.length == 0)
            return List.of();

        var result = new ArrayList<String>();
        for (var tp : params) {
            var sb = new StringBuilder(tp.getName());
            Type[] bounds = tp.getBounds();
            if (bounds.length > 0 && !bounds[0].equals(Object.class)) {
                sb.append(" extends ");
                var joiner = new StringJoiner(" & ");
                for (var bound : bounds)
                    joiner.add(TypeNameFormatter.format(bound));
                sb.append(joiner);
            }
            result.add(sb.toString());
        }
        return result;
    }

    private static String extractSuperclass(Class<?> clazz) {
        Class<?> superclass = clazz.getSuperclass();
        if (superclass == null || superclass == Object.class)
            return null;
        return superclass.getName();
    }

    private static List<String> extractInterfaces(Class<?> clazz) {
        return Arrays.stream(clazz.getInterfaces()).map(Class::getName).toList();
    }

    private static List<FieldInfo> extractFields(Class<?> clazz) {
        var result = new ArrayList<FieldInfo>();
        try {
            for (var field : clazz.getDeclaredFields()) {
                if (field.isSynthetic())
                    continue;
                try {
                    int mod = field.getModifiers();
                    result.add(new FieldInfo(
                            field.getName(),
                            TypeNameFormatter.format(field.getGenericType()),
                            ModifierHelper.visibilityFrom(mod),
                            ModifierHelper.extractModifiers(mod),
                            extractAnnotations(field.getAnnotations())));
                } catch (NoClassDefFoundError e) {
                    System.err.println("[WARN] Skipping field: " + field.getName() + " — " + e.getMessage());
                }
            }
        } catch (NoClassDefFoundError e) {
            System.err.println("[WARN] Could not read fields of " + clazz.getName() + " — " + e.getMessage());
        }
        return result;
    }

    private static List<MethodInfo> extractMethods(Class<?> clazz) {
        var result = new ArrayList<MethodInfo>();
        try {
            for (var method : clazz.getDeclaredMethods()) {
                if (method.isSynthetic())
                    continue;
                try {
                    int mod = method.getModifiers();
                    result.add(new MethodInfo(
                            method.getName(),
                            TypeNameFormatter.format(method.getGenericReturnType()),
                            extractParameters(method.getParameters(), method.getGenericParameterTypes()),
                            Arrays.stream(method.getExceptionTypes()).map(Class::getName).toList(),
                            ModifierHelper.visibilityFrom(mod),
                            ModifierHelper.extractModifiers(mod),
                            extractAnnotations(method.getAnnotations())));
                } catch (NoClassDefFoundError e) {
                    System.err.println("[WARN] Skipping method: " + method.getName() + " — " + e.getMessage());
                }
            }
        } catch (NoClassDefFoundError e) {
            System.err.println("[WARN] Could not read methods of " + clazz.getName() + " — " + e.getMessage());
        }
        result.sort(Comparator.comparing(MethodInfo::name));
        return result;
    }

    private static List<ConstructorInfo> extractConstructors(Class<?> clazz) {
        var result = new ArrayList<ConstructorInfo>();
        try {
            for (var ctor : clazz.getDeclaredConstructors()) {
                if (ctor.isSynthetic())
                    continue;
                try {
                    int mod = ctor.getModifiers();
                    result.add(new ConstructorInfo(
                            extractParameters(ctor.getParameters(), ctor.getGenericParameterTypes()),
                            ModifierHelper.visibilityFrom(mod),
                            extractAnnotations(ctor.getAnnotations())));
                } catch (NoClassDefFoundError e) {
                    System.err.println("[WARN] Skipping constructor — " + e.getMessage());
                }
            }
        } catch (NoClassDefFoundError e) {
            System.err.println("[WARN] Could not read constructors of " + clazz.getName() + " — " + e.getMessage());
        }
        return result;
    }

    private static List<ParameterInfo> extractParameters(Parameter[] params, Type[] genericTypes) {
        var result = new ArrayList<ParameterInfo>();
        for (int i = 0; i < params.length; i++) {
            String name = params[i].isNamePresent() ? params[i].getName() : "arg" + i;
            String typeName = (i < genericTypes.length)
                    ? TypeNameFormatter.format(genericTypes[i])
                    : TypeNameFormatter.format(params[i].getType());
            result.add(new ParameterInfo(name, typeName));
        }
        return result;
    }

    private static List<String> extractAnnotations(Annotation[] annotations) {
        return Arrays.stream(annotations)
                .map(a -> "@" + a.annotationType().getSimpleName())
                .toList();
    }

    private static void collectRelationships(Class<?> clazz, Set<String> jarClassNames,
            List<Relationship> relationships) {
        String from = clazz.getName();
        var linkedTypes = new HashSet<String>();

        // extends
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class
                && jarClassNames.contains(superclass.getName())) {
            relationships.add(new Relationship(from, superclass.getName(), RelationshipKind.EXTENDS));
            linkedTypes.add(superclass.getName());
        }

        // implements
        for (var iface : clazz.getInterfaces()) {
            if (jarClassNames.contains(iface.getName())) {
                relationships.add(new Relationship(from, iface.getName(), RelationshipKind.IMPLEMENTS));
                linkedTypes.add(iface.getName());
            }
        }

        // uses — field types, method param/return types
        var usedTypes = new HashSet<String>();
        for (var field : clazz.getDeclaredFields()) {
            if (!field.isSynthetic())
                collectRawTypes(field.getGenericType(), usedTypes);
        }
        for (var method : clazz.getDeclaredMethods()) {
            if (method.isSynthetic())
                continue;
            collectRawTypes(method.getGenericReturnType(), usedTypes);
            for (var paramType : method.getGenericParameterTypes())
                collectRawTypes(paramType, usedTypes);
        }

        for (var used : usedTypes) {
            if (!used.equals(from) && !linkedTypes.contains(used) && jarClassNames.contains(used)) {
                relationships.add(new Relationship(from, used, RelationshipKind.USES));
            }
        }
    }

    // Recursively extracts raw class names from a generic type
    private static void collectRawTypes(Type type, Set<String> out) {
        switch (type) {
            case Class<?> c -> {
                if (c.isArray())
                    collectRawTypes(c.getComponentType(), out);
                else if (!c.isPrimitive())
                    out.add(c.getName());
            }
            case ParameterizedType pt -> {
                collectRawTypes(pt.getRawType(), out);
                for (var arg : pt.getActualTypeArguments())
                    collectRawTypes(arg, out);
            }
            case GenericArrayType gat -> collectRawTypes(gat.getGenericComponentType(), out);
            case WildcardType wt -> {
                for (var bound : wt.getUpperBounds())
                    collectRawTypes(bound, out);
                for (var bound : wt.getLowerBounds())
                    collectRawTypes(bound, out);
            }
            default -> {
            }
        }
    }
}
