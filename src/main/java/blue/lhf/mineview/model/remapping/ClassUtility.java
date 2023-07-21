package blue.lhf.mineview.model.remapping;

public final class ClassUtility {
    private ClassUtility() {
    }

    public static String toInternalName(final String className) {
        return className.replace('.', '/');
    }

    public static String toDescriptor(final String className) {
        if (className.isEmpty()) return className;
        final int arrayCount = className.split("\\[]", -1).length - 1;
        if (arrayCount > 0) {
            return "[".repeat(arrayCount) + toDescriptor(className.substring(0, className.length() - arrayCount * 2));
        }
        return switch (className) {
            case "void" -> "V";
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "char" -> "C";
            case "short" -> "S";
            case "int" -> "I";
            case "float" -> "F";
            case "long" -> "J";
            case "double" -> "D";
            default -> "L" + toInternalName(className) + ";";
        };
    }
}
