package com.smartsorting.tile;

import java.util.Locale;

public enum RedstoneMode {

    ALWAYS,
    WITH_SIGNAL,
    WITHOUT_SIGNAL,
    NEVER;

    public boolean allows(boolean powered) {
        switch (this) {
            case WITH_SIGNAL:
                return powered;
            case WITHOUT_SIGNAL:
                return !powered;
            case NEVER:
                return false;
            default:
                return true;
        }
    }

    public RedstoneMode cycle(boolean backwards) {
        RedstoneMode[] values = values();
        return values[Math.floorMod(ordinal() + (backwards ? -1 : 1), values.length)];
    }

    public String translationKey() {
        return "smartsorting.redstone." + name().toLowerCase(Locale.ROOT);
    }

    public static RedstoneMode fromOrdinal(int ordinal) {
        RedstoneMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ALWAYS;
    }
}
