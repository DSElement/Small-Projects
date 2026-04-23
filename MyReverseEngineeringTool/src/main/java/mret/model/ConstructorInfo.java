package mret.model;

import java.util.List;

public record ConstructorInfo(
        List<ParameterInfo> parameters,
        Visibility visibility,
        List<String> annotations
) {}
