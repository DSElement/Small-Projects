package mret.model;

public enum Visibility {
    PUBLIC,
    PROTECTED,
    PACKAGE_PRIVATE,
    PRIVATE;

    public String toUmlSymbol() {
        return switch (this) {
            case PUBLIC          -> "+";
            case PROTECTED       -> "#";
            case PACKAGE_PRIVATE -> "~";
            case PRIVATE         -> "-";
        };
    }

    public String toPlantUmlSymbol() {
        return toUmlSymbol();
    }
}
