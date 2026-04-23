package mret.renderer;

import mret.model.*;

import java.util.stream.Collectors;

public final class PlantUmlRenderer implements Renderer {

    @Override
    public String render(ClassDiagram diagram) {
        var sb = new StringBuilder();

        sb.append("@startuml\n\n");
        sb.append("skinparam classAttributeIconSize 0\n");
        sb.append("skinparam monochrome false\n");
        sb.append("skinparam shadowing true\n");
        sb.append("skinparam linetype ortho\n\n");

        var byPackage = diagram.types().stream()
                .collect(Collectors.groupingBy(TypeInfo::packageName));

        for (var entry : byPackage.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .toList()) {

            sb.append("package ").append(entry.getKey()).append(" {\n\n");
            for (var type : entry.getValue()) {
                renderType(type, sb);
                sb.append("\n");
            }
            sb.append("}\n\n");
        }

        if (!diagram.relationships().isEmpty()) {
            sb.append("' ── Relationships ──\n\n");
            for (var rel : diagram.relationships()) renderRelationship(rel, sb);
            sb.append("\n");
        }

        sb.append("@enduml\n");
        return sb.toString();
    }

    private void renderType(TypeInfo type, StringBuilder sb) {
        for (var ann : type.annotations()) sb.append("  ").append(ann).append("\n");

        sb.append("  ").append(type.kind().plantUmlKeyword()).append(" ");
        sb.append("\"").append(type.simpleName());
        if (!type.typeParameters().isEmpty())
            sb.append("<").append(String.join(", ", type.typeParameters())).append(">");
        sb.append("\"");
        sb.append(" as ").append(type.fullyQualifiedName());
        sb.append(" {\n");

        for (var field : type.fields()) renderField(field, sb);

        if (!type.fields().isEmpty()
                && (!type.constructors().isEmpty() || !type.methods().isEmpty())) {
            sb.append("    ..\n");
        }

        for (var ctor : type.constructors()) renderConstructor(type.simpleName(), ctor, sb);
        for (var method : type.methods()) renderMethod(method, sb);

        sb.append("  }\n");
    }

    private void renderField(FieldInfo field, StringBuilder sb) {
        sb.append("    ").append(field.visibility().toPlantUmlSymbol());
        if (field.modifiers().contains("static")) sb.append("{static} ");
        else if (field.modifiers().contains("abstract")) sb.append("{abstract} ");
        else sb.append(" ");
        sb.append(field.name()).append(" : ").append(field.typeName()).append("\n");
    }

    private void renderConstructor(String simpleName, ConstructorInfo ctor, StringBuilder sb) {
        sb.append("    ").append(ctor.visibility().toPlantUmlSymbol()).append(" ");
        sb.append("<<create>> ").append(simpleName).append("(");
        sb.append(ctor.parameters().stream()
                .map(p -> p.name() + " : " + p.typeName())
                .collect(Collectors.joining(", ")));
        sb.append(")\n");
    }

    private void renderMethod(MethodInfo method, StringBuilder sb) {
        sb.append("    ").append(method.visibility().toPlantUmlSymbol());
        if (method.modifiers().contains("static")) sb.append("{static} ");
        else if (method.modifiers().contains("abstract")) sb.append("{abstract} ");
        else sb.append(" ");
        sb.append(method.name()).append("(");
        sb.append(method.parameters().stream()
                .map(p -> p.name() + " : " + p.typeName())
                .collect(Collectors.joining(", ")));
        sb.append(") : ").append(method.returnTypeName()).append("\n");
    }

    private void renderRelationship(Relationship rel, StringBuilder sb) {
        sb.append(rel.fromType());
        sb.append(" ").append(rel.kind().plantUmlArrow()).append(" ");
        sb.append(rel.toType());
        if (rel.kind() == RelationshipKind.USES) sb.append(" : uses");
        sb.append("\n");
    }
}
