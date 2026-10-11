package com.smartsorting.item;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class Tooltips {

    private Tooltips() {
    }

    public static void add(String translationKey, List<Component> tooltip) {
        for (int i = 0; Language.getInstance().has(translationKey + ".tooltip." + i); i++) {
            tooltip.add(Component.translatable(translationKey + ".tooltip." + i).withStyle(ChatFormatting.GRAY));
        }
    }
}
