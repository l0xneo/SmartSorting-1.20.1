package com.smartsorting.block;

import net.minecraft.util.StringRepresentable;

public enum MoverSide implements StringRepresentable {
    OFF("off"),
    TAKE("take"),
    GIVE("give");

    private final String name;

    MoverSide(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
