package mret.model;

import java.util.List;
import java.util.Set;

public record TypeInfo(
        TypeKind kind,
        Visibility visibility,
        Set<String> modifiers,
        String packageName,
        String fullyQualifiedName,
        String simpleName,
        List<String> typeParameters,
        String superclass,
        List<String> interfaces,
        List<FieldInfo> fields,
        List<MethodInfo> methods,
        List<ConstructorInfo> constructors,
        List<String> annotations
) {}
