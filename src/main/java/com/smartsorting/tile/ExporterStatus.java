package com.smartsorting.tile;

public enum ExporterStatus {

    WORKING(0xFF3FAE4F),
    SOURCE_EMPTY(0xFF9AA3AD),
    NO_SOURCE(0xFFE0B13A),
    NO_DESTINATION(0xFFE0B13A),
    BLOCKED(0xFFD2443C),
    DISABLED(0xFF9AA3AD),
    STUCK(0xFFD2443C),
    PIPE_FULL(0xFFE0B13A);

    public final int color;

    ExporterStatus(int color) {
        this.color = color;
    }

    public String translationKey() {
        return "smartsorting.status." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static ExporterStatus fromOrdinal(int ordinal) {
        ExporterStatus[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SOURCE_EMPTY;
    }
}
