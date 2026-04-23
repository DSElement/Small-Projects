package mret.model;

import java.util.List;
import java.util.Set;

public record MethodInfo(
        String name,
        String returnTypeName,
        List<ParameterInfo> parameters,
        List<String> thrownExceptions,
        Visibility visibility,
        Set<String> modifiers,
        List<String> annotations
) {}
