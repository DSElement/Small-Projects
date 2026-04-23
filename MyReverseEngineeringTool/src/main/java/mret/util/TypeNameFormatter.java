package mret.util;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.StringJoiner;

public final class TypeNameFormatter {

    private TypeNameFormatter() {}

    public static String format(Type type) {
        if (type == null) return "void";

        return switch (type) {
            case Class<?> clazz      -> formatClass(clazz);
            case ParameterizedType pt -> formatParameterized(pt);
            case GenericArrayType gat -> format(gat.getGenericComponentType()) + "[]";
            case TypeVariable<?> tv   -> tv.getName();
            case WildcardType wt      -> formatWildcard(wt);
            default                   -> simplify(type.getTypeName());
        };
    }

    private static String formatClass(Class<?> clazz) {
        if (clazz.isArray()) return format(clazz.getComponentType()) + "[]";
        return simplify(clazz.getName());
    }

    private static String formatParameterized(ParameterizedType pt) {
        var sb = new StringBuilder();
        sb.append(simplify(pt.getRawType().getTypeName()));

        Type[] args = pt.getActualTypeArguments();
        if (args.length > 0) {
            var joiner = new StringJoiner(", ", "<", ">");
            for (var arg : args) joiner.add(format(arg));
            sb.append(joiner);
        }
        return sb.toString();
    }

    private static String formatWildcard(WildcardType wt) {
        Type[] upper = wt.getUpperBounds();
        Type[] lower = wt.getLowerBounds();

        if (lower.length > 0) return "? super " + format(lower[0]);
        if (upper.length > 0 && !upper[0].equals(Object.class)) return "? extends " + format(upper[0]);
        return "?";
    }

    public static String simplify(String typeName) {
        return typeName
                .replaceAll("\\bjava\\.lang\\.", "")
                .replaceAll("\\bjava\\.util\\.", "")
                .replaceAll("\\bjava\\.io\\.", "");
    }
}
