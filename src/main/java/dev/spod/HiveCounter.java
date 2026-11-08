package dev.spod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Pixel counters fixed to hive tops and front upper-left corners. */
public final class HiveCounter {
    private static final String[] GLYPHS = {
            "111101101101111", "010110010010111", "111001111100111", "111001111001111",
            "101101111001001", "111100111001111", "111100111101111", "111001001001001",
            "111101111101111", "111101111001111", "111001010000010"
    };
    private static final float PIXEL = 0.028F;

    private HiveCounter() {}

    public static void onRenderLevel(RenderLevelStageEvent event) {
        // Draw into the scene before glass/translucency, using the active world model-view matrix.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !OverlayControls.visible(mc) || HiveData.positions().isEmpty()) {
            return;
        }
        var camera = event.getCamera().getPosition();
        PoseStack pose = new PoseStack();
        var buffers = mc.renderBuffers().bufferSource();
        var quads = buffers.getBuffer(RenderType.debugQuads());
        for (var pos : HiveData.positions()) {
            var state = mc.level.getBlockState(pos);
            if (!(state.getBlock() instanceof BeehiveBlock)
                    || pos.distToCenterSqr(camera) > HiveData.RANGE * HiveData.RANGE
                    || !event.getFrustum().isVisible(new AABB(pos).inflate(0.01))) {
                continue;
            }
            int count = HiveData.count(pos);
            String text = count < 0 ? "?" : Integer.toString(count);
            float glyphWidth = (text.length() * 4 - 1) * PIXEL;
            float halfWidth = Math.max(0.22F, glyphWidth + 0.08F) / 2;
            var facing = state.getValue(BeehiveBlock.FACING);
            pose.pushPose();
            pose.translate(pos.getX() + 0.5 - camera.x, pos.getY() - camera.y, pos.getZ() + 0.5 - camera.z);
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            if (camera.y > pos.getY() + 1) {
                pose.pushPose();
                pose.translate(0, 1.002, 0);
                drawCounter(pose, quads, text, count, glyphWidth, halfWidth);
                pose.popPose();
            }
            double frontDistance = (camera.x - pos.getX() - 0.5) * facing.getStepX()
                    + (camera.z - pos.getZ() - 0.5) * facing.getStepZ();
            if (frontDistance > 0.502) {
                pose.pushPose();
                // Five-percent left inset and a small top inset, as seen facing the hive entrance.
                pose.translate(-0.45 + halfWidth, 0.84, 0.502);
                pose.mulPose(Axis.XP.rotationDegrees(90));
                drawCounter(pose, quads, text, count, glyphWidth, halfWidth);
                pose.popPose();
            }
            pose.popPose();
        }
        buffers.endBatch(RenderType.debugQuads());
    }

    private static void drawCounter(PoseStack pose, VertexConsumer quads, String text, int count,
                                    float glyphWidth, float halfWidth) {
        float inner = halfWidth - 0.012F;
        float left = -glyphWidth / 2;
        float right = glyphWidth / 2;
        float top = -2.5F * PIXEL;
        float bottom = 2.5F * PIXEL;
        // Tile one plane instead of stacking translucent layers: no depth flicker over the digits.
        quad(pose, quads, -halfWidth, -0.115F, halfWidth, -0.103F, 0xFF78603C);
        quad(pose, quads, -halfWidth, 0.103F, halfWidth, 0.115F, 0xFF78603C);
        quad(pose, quads, -halfWidth, -0.103F, -inner, 0.103F, 0xFF78603C);
        quad(pose, quads, inner, -0.103F, halfWidth, 0.103F, 0xFF78603C);
        quad(pose, quads, -inner, -0.103F, left, 0.103F, 0xFF25291E);
        quad(pose, quads, right, -0.103F, inner, 0.103F, 0xFF25291E);
        quad(pose, quads, left, -0.103F, right, top, 0xFF25291E);
        quad(pose, quads, left, bottom, right, 0.103F, 0xFF25291E);
        for (int row = 0; row < 5; row++) {
            for (int column = 0; column < text.length() * 4 - 1; column++) {
                char character = text.charAt(column / 4);
                String glyph = GLYPHS[character == '?' ? 10 : character - '0'];
                boolean lit = column % 4 < 3 && glyph.charAt(row * 3 + column % 4) == '1';
                float x = left + column * PIXEL;
                float z = top + row * PIXEL;
                quad(pose, quads, x, z, x + PIXEL, z + PIXEL,
                        lit ? (count < 0 ? 0xFFE1B56B : 0xFF91DD7A) : 0xFF25291E);
            }
        }
    }

    private static void quad(PoseStack pose, VertexConsumer buffer, float x0, float z0, float x1, float z1, int color) {
        buffer.addVertex(pose.last(), x0, 0, z0).setColor(color);
        buffer.addVertex(pose.last(), x0, 0, z1).setColor(color);
        buffer.addVertex(pose.last(), x1, 0, z1).setColor(color);
        buffer.addVertex(pose.last(), x1, 0, z0).setColor(color);
    }
}
