package com.smartsorting;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ModConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue ITEMS_PER_TRANSFER;
    public static final ForgeConfigSpec.IntValue IDLE_BACKOFF_MAX;
    public static final ForgeConfigSpec.IntValue MAX_NETWORK_SIZE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        ITEMS_PER_TRANSFER = b.comment("Items per transfer without stack upgrades. Each stack upgrade multiplies it by 4 (max 64).")
                .defineInRange("itemsPerTransfer", 1, 1, 64);
        IDLE_BACKOFF_MAX = b.comment("Longest wait (ticks) an idle exporter backs off to before checking again.")
                .defineInRange("idleBackoffMax", 40, 1, 400);
        MAX_NETWORK_SIZE = b.comment("Maximum number of pipes scanned per connected pipe network.")
                .defineInRange("maxNetworkSize", 2048, 16, 16384);
        SPEC = b.build();
    }

    private ModConfig() {
    }
}
