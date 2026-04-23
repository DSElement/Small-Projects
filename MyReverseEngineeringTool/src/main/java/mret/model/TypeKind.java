package mret.model;

public enum TypeKind {
    CLASS,
    ABSTRACT_CLASS,
    INTERFACE,
    ENUM,
    ANNOTATION;

    public String label() {
        return switch (this) {
            case CLASS          -> "class";
            case ABSTRACT_CLASS -> "abstract class";
            case INTERFACE      -> "interface";
            case ENUM           -> "enum";
            case ANNOTATION     -> "annotation";
        };
    }

    public String plantUmlKeyword() {
        return switch (this) {
            case CLASS          -> "class";
            case ABSTRACT_CLASS -> "abstract class";
            case INTERFACE      -> "interface";
            case ENUM           -> "enum";
            case ANNOTATION     -> "annotation";
        };
    }

    public String stereotype() {
        return switch (this) {
            case CLASS          -> "";
            case ABSTRACT_CLASS -> "«abstract»";
            case INTERFACE      -> "«interface»";
            case ENUM           -> "«enum»";
            case ANNOTATION     -> "«annotation»";
        };
    }
}
