package mret.model;

import java.util.List;

public record ClassDiagram(
        List<TypeInfo> types,
        List<Relationship> relationships
) {}
