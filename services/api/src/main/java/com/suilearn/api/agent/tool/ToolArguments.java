package com.suilearn.api.agent.tool;

import java.util.List;
import java.util.Map;

public final class ToolArguments {
    private ToolArguments() {}

    public static String requiredString(Map<String, Object> args, String name, int maxLength) {
        Object value = args.get(name);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        text = text.strip();
        if (text.length() > maxLength) {
            throw new IllegalArgumentException(name + " is too long");
        }
        return text;
    }

    public static String optionalString(Map<String, Object> args, String name, int maxLength) {
        Object value = args.get(name);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException(name + " must be a string");
        }
        text = text.strip();
        if (text.isBlank()) {
            return null;
        }
        if (text.length() > maxLength) {
            throw new IllegalArgumentException(name + " is too long");
        }
        return text;
    }

    public static int integer(Map<String, Object> args, String name, int defaultValue, int min, int max) {
        Object value = args.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
        int parsed = number.intValue();
        if (parsed < min || parsed > max) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
        }
        return parsed;
    }

    public static double decimal(Map<String, Object> args, String name, double defaultValue, double min, double max) {
        Object value = args.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(name + " must be a number");
        }
        double parsed = number.doubleValue();
        if (!Double.isFinite(parsed) || parsed < min || parsed > max) {
            throw new IllegalArgumentException(name + " is out of range");
        }
        return parsed;
    }

    public static boolean bool(Map<String, Object> args, String name, boolean defaultValue) {
        Object value = args.get(name);
        return value == null ? defaultValue : Boolean.parseBoolean(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> mapList(Map<String, Object> args, String name) {
        Object value = args.get(name);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(name + " must be a list");
        }
        return list.stream().map(item -> (Map<String, Object>) item).toList();
    }

}
