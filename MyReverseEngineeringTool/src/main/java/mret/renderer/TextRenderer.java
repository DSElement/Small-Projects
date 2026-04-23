package mret.renderer;

import mret.model.*;

import java.util.stream.Collectors;

public final class TextRenderer implements Renderer {

    private static final String DOUBLE_LINE = "═".repeat(60);
    private static final String SINGLE_LINE = "─".repeat(60);

    @Override
    public String render(ClassDiagram diagram) {
        var sb = new StringBuilder();

        sb.append("""
                ╔════════════════════════════════════════════════════════════╗
                ║                   CLASS DIAGRAM                          ║
                ╚════════════════════════════════════════════════════════════╝
                """);
        sb.append("\n");

        for (var type : diagram.types()) {
            renderType(type, sb);
            sb.append("\n");
        }

        renderRelationships(diagram, sb);
        return sb.toString();
    }

    private void renderType(TypeInfo type, StringBuilder sb) {
        sb.append(DOUBLE_LINE).append("\n");

        String stereo = type.kind().stereotype();
        if (!stereo.isEmpty())
            sb.append(stereo).append(" ");
        sb.append(type.fullyQualifiedName());
        if (!type.typeParameters().isEmpty()) {
            sb.append("<").append(String.join(", ", type.typeParameters())).append(">");
        }
        sb.append("\n");
        sb.append(DOUBLE_LINE).append("\n");

        if (!type.annotations().isEmpty())
            sb.append("Annotations: ").append(String.join(", ", type.annotations())).append("\n");
        if (!type.modifiers().isEmpty())
            sb.append("Modifiers: ").append(String.join(", ", type.modifiers())).append("\n");
        if (type.superclass() != null)
            sb.append("Extends: ").append(type.superclass()).append("\n");
        if (!type.interfaces().isEmpty()) {
            String keyword = type.kind() == TypeKind.INTERFACE ? "Extends" : "Implements";
            sb.append(keyword).append(": ").append(String.join(", ", type.interfaces())).append("\n");
        }

        if (!type.fields().isEmpty()) {
            sb.append(SINGLE_LINE).append("\n");
            sb.append("Fields:\n");
            for (var field : type.fields())
                renderField(field, sb);
        }

        if (!type.constructors().isEmpty()) {
            sb.append(SINGLE_LINE).append("\n");
            sb.append("Constructors:\n");
            for (var ctor : type.constructors())
                renderConstructor(type.simpleName(), ctor, sb);
        }

        if (!type.methods().isEmpty()) {
            sb.append(SINGLE_LINE).append("\n");
            sb.append("Methods:\n");
            for (var method : type.methods())
                renderMethod(method, sb);
        }

        sb.append(DOUBLE_LINE).append("\n");
    }

    private void renderField(FieldInfo field, StringBuilder sb) {
        sb.append("  ").append(field.visibility().toUmlSymbol()).append(" ");
        for (var ann : field.annotations())
            sb.append(ann).append(" ");
        sb.append(field.name()).append(" : ").append(field.typeName());
        if (!field.modifiers().isEmpty())
            sb.append(" {").append(String.join(", ", field.modifiers())).append("}");
        sb.append("\n");
    }

    private void renderConstructor(String simpleName, ConstructorInfo ctor, StringBuilder sb) {
        sb.append("  ").append(ctor.visibility().toUmlSymbol()).append(" ");
        for (var ann : ctor.annotations())
            sb.append(ann).append(" ");
        sb.append(simpleName).append("(");
        sb.append(ctor.parameters().stream().map(ParameterInfo::typeName).collect(Collectors.joining(", ")));
        sb.append(")\n");
    }

    private void renderMethod(MethodInfo method, StringBuilder sb) {
        sb.append("  ").append(method.visibility().toUmlSymbol()).append(" ");
        for (var ann : method.annotations())
            sb.append(ann).append(" ");
        sb.append(method.name()).append("(");
        sb.append(method.parameters().stream().map(ParameterInfo::typeName).collect(Collectors.joining(", ")));
        sb.append(") : ").append(method.returnTypeName());
        if (!method.modifiers().isEmpty())
            sb.append(" {").append(String.join(", ", method.modifiers())).append("}");
        if (!method.thrownExceptions().isEmpty())
            sb.append(" throws ").append(String.join(", ", method.thrownExceptions()));
        sb.append("\n");
    }

    private void renderRelationships(ClassDiagram diagram, StringBuilder sb) {
        if (diagram.relationships().isEmpty())
            return;

        sb.append("\n").append(DOUBLE_LINE).append("\n");
        sb.append("RELATIONSHIPS\n");
        sb.append(DOUBLE_LINE).append("\n");

        for (var rel : diagram.relationships()) {
            String arrow = switch (rel.kind()) {
                case EXTENDS -> "──▶";
                case IMPLEMENTS -> "--▷";
                case USES -> "-->";
            };
            sb.append("  ").append(simpleName(rel.fromType()))
                    .append(" ").append(arrow).append(" ")
                    .append(simpleName(rel.toType()))
                    .append("  [").append(rel.kind().label()).append("]")
                    .append("\n");
        }

        sb.append(DOUBLE_LINE).append("\n");
    }

    private static String simpleName(String fqcn) {
        int dot = fqcn.lastIndexOf('.');
        return dot >= 0 ? fqcn.substring(dot + 1) : fqcn;
    }
}
