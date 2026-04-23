package mret.util;

import mret.model.Visibility;

import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ModifierHelper {

    private ModifierHelper() {}

    public static Visibility visibilityFrom(int modifiers) {
        if (Modifier.isPublic(modifiers))    return Visibility.PUBLIC;
        if (Modifier.isProtected(modifiers)) return Visibility.PROTECTED;
        if (Modifier.isPrivate(modifiers))   return Visibility.PRIVATE;
        return Visibility.PACKAGE_PRIVATE;
    }

    public static Set<String> extractModifiers(int modifiers) {
        var result = new LinkedHashSet<String>();
        if (Modifier.isStatic(modifiers))       result.add("static");
        if (Modifier.isFinal(modifiers))        result.add("final");
        if (Modifier.isAbstract(modifiers))     result.add("abstract");
        if (Modifier.isSynchronized(modifiers)) result.add("synchronized");
        if (Modifier.isVolatile(modifiers))     result.add("volatile");
        if (Modifier.isTransient(modifiers))    result.add("transient");
        if (Modifier.isNative(modifiers))       result.add("native");
        if (Modifier.isStrict(modifiers))       result.add("strictfp");
        return result;
    }
}
