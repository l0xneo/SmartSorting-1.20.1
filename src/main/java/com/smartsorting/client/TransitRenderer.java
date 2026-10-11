package com.smartsorting.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.smartsorting.SmartSorting;
import com.smartsorting.net.MsgTransitAdd;
import com.smartsorting.net.MsgTransitChange;
import com.smartsorting.transport.PathMath;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = SmartSorting.MOD_ID, value = Dist.CLIENT)
public final class TransitRenderer {

    private static final float SCALE = 0.2F;
    private static final int CYCLE = 60;

    private static final class Shown {
        final ItemStack stack;
        final List<BlockPos> path;
        float progress;
        float speed;
        float end;
        long since;

        Shown(ItemStack stack, List<BlockPos> path) {
            this.stack = stack;
            this.path = path;
        }

        void set(float progress, float speed, float end, long now) {
            this.progress = progress;
            this.speed = speed;
            this.end = end;
            this.since = now;
        }
    }

    private static final Map<Integer, Shown> ITEMS = new HashMap<>();
    private static Level shownLevel;

    private TransitRenderer() {
    }

    private static long now() {
        Level level = Minecraft.getInstance().level;
        if (level != shownLevel) {
            ITEMS.clear();
            shownLevel = level;
        }
        return level == null ? 0 : level.getGameTime();
    }

    public static void add(MsgTransitAdd message) {
        long now = now();
        if (message.reset()) {
            ITEMS.clear();
        }
        for (MsgTransitAdd.Data data : message.items()) {
            Shown shown = new Shown(data.stack(), data.path());
            shown.set(data.progress(), data.speed(), data.end(), now);
            ITEMS.put(data.id(), shown);
        }
    }

    public static void change(MsgTransitChange message) {
        long now = now();
        for (MsgTransitChange.Change change : message.changes()) {
            Shown shown = ITEMS.get(change.id());
            if (shown != null) {
                shown.set(change.progress(), change.speed(), change.end(), now);
            }
        }
        message.removed().forEach(ITEMS::remove);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || ITEMS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = now();
        if (mc.level == null) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick();
        float cycleAge = now % CYCLE + partial;

        for (Map.Entry<Integer, Shown> entry : ITEMS.entrySet()) {
            Shown shown = entry.getValue();
            float progress = Math.min(shown.end, shown.progress + (now - shown.since + partial) * shown.speed);
            Vec3 pos = PathMath.point(shown.path, progress);
            if (pos.distanceToSqr(camera) > 96 * 96) {
                continue;
            }
            float age = cycleAge + entry.getKey() * 7;
            pose.pushPose();
            float bob = Mth.sin(age * Mth.TWO_PI * 3 / CYCLE) * 0.015F;
            pose.translate(pos.x - camera.x, pos.y - camera.y + bob, pos.z - camera.z);
            pose.mulPose(Axis.YP.rotationDegrees(age * 360 / CYCLE));
            pose.scale(SCALE, SCALE, SCALE);
            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(pos));
            mc.getItemRenderer().renderStatic(shown.stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                    pose, buffers, mc.level, 0);
            pose.popPose();
        }
        buffers.endBatch();
    }
}
