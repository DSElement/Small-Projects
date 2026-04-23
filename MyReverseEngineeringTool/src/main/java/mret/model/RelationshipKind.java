package mret.model;

public enum RelationshipKind {
    EXTENDS,
    IMPLEMENTS,
    USES;

    public String label() {
        return switch (this) {
            case EXTENDS    -> "extends";
            case IMPLEMENTS -> "implements";
            case USES       -> "uses";
        };
    }

    public String plantUmlArrow() {
        return switch (this) {
            case EXTENDS    -> "--|>";
            case IMPLEMENTS -> "..|>";
            case USES       -> "..>";
        };
    }
}
