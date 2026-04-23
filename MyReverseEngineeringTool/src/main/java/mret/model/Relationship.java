package mret.model;

public record Relationship(
        String fromType,
        String toType,
        RelationshipKind kind
) {}
