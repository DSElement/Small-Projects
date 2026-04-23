package mret.model;

import java.util.List;
import java.util.Set;

public record FieldInfo(
        String name,
        String typeName,
        Visibility visibility,
        Set<String> modifiers,
        List<String> annotations
) {}
