package dev.spod;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Checks client-visible ground placement and current hostile-spawn lighting. */
public final class SpawnOverlay {
    private static final int HALF_WIDTH = 15;
    private static final String LABEL = "spawnable = true";
    private static final float LABEL_SCALE = 0.009F;
    private static final List<BlockPos> SURFACES = new ArrayList<>();
    private static ClientLevel level;
    private static int ticksUntilScan;

    private SpawnOverlay() {}

    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (level != mc.level) {
            level = mc.level;
            SURFACES.clear();
            ticksUntilScan = 0;
        }
        if (level == null || mc.player == null || !OverlayControls.visible(mc)) {
            SURFACES.clear();
            ticksUntilScan = 0;
            return;
        }
        if (ticksUntilScan-- > 0) {
            return;
        }
        ticksUntilScan = 9;
        SURFACES.clear();
        BlockPos center = mc.player.blockPosition();
        BlockPos min = center.offset(-HALF_WIDTH, -6, -HALF_WIDTH);
        BlockPos max = center.offset(HALF_WIDTH - 1, 6, HALF_WIDTH - 1);
        for (BlockPos floor : BlockPos.betweenClosed(min, max)) {
            if (isSpawnableSurface(level, floor)) {
                SURFACES.add(floor.immutable());
            }
        }
    }

    static boolean isSpawnableSurface(ClientLevel world, BlockPos floor) {
        BlockPos feet = floor.above();
        if (floor.getY() < world.getMinBuildHeight() || feet.getY() + 1 >= world.getMaxBuildHeight()
                || !world.hasChunkAt(floor)) {
            return false;
        }
        int light = world.getBrightness(LightLayer.BLOCK, feet);
        var dimension = world.dimensionType();
        // Match Monster.isDarkEnoughToSpawn's current effective-light check at its maximum
        // random threshold: passing means the lighting test has a nonzero chance to succeed.
        int brightness = world.isThundering() ? world.getMaxLocalRawBrightness(feet, 10)
                : world.getMaxLocalRawBrightness(feet);
        if (world.getDifficulty() == Difficulty.PEACEFUL || light > dimension.monsterSpawnBlockLightLimit()
                || brightness > dimension.monsterSpawnLightTest().getMaxValue()) {
            return false;
        }
        // Use the game's ground-placement and collision rules for a standard two-block-tall hostile mob.
        return SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(world, feet, EntityType.ZOMBIE)
                && world.noCollision(EntityType.ZOMBIE.getSpawnAABB(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5));
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != level || level == null || !OverlayControls.visible(mc) || SURFACES.isEmpty()) {
            return;
        }
        var camera = event.getCamera().getPosition();
        PoseStack pose = new PoseStack();
        pose.mulPose(event.getModelViewMatrix());
        pose.translate(-camera.x, -camera.y, -camera.z);
        var buffers = mc.renderBuffers().bufferSource();
        var lines = buffers.getBuffer(RenderType.lines());
        List<BlockPos> visible = new ArrayList<>();
        for (BlockPos floor : SURFACES) {
            AABB box = new AABB(floor).inflate(0.002);
            if (event.getFrustum().isVisible(box)) {
                visible.add(floor);
                // Normal depth-tested lines keep hidden underground surfaces out of the view.
                LevelRenderer.renderLineBox(pose, lines, box, 1, 0.1F, 0.1F, 1);
            }
        }
        buffers.endBatch(RenderType.lines());
        // World-space labels share the outlines' depth testing, so walls hide covered surfaces.
        for (BlockPos floor : visible) {
            pose.pushPose();
            pose.translate(floor.getX() + 0.5, floor.getY() + 1.12, floor.getZ() + 0.5);
            pose.mulPose(event.getCamera().rotation());
            pose.scale(-LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);
            mc.font.drawInBatch(LABEL, -mc.font.width(LABEL) / 2.0F, 0, 0xFFFF5555, false,
                    pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0x60000000, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        buffers.endBatch();
    }
}
