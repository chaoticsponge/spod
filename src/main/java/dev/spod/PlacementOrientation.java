package dev.spod;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class PlacementOrientation {
    private static final Direction[] DIRECTIONS = {Direction.NORTH, Direction.EAST, Direction.SOUTH,
            Direction.WEST, Direction.UP, Direction.DOWN};
    private static final KeyMapping CYCLE = new KeyMapping("key.spod.placement_facing",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.spod");
    private static ClientLevel world;
    private static int selection;

    private PlacementOrientation() {}

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(CYCLE);
    }

    public static Direction facing() {
        return selection == 0 ? null : DIRECTIONS[selection - 1];
    }

    public static boolean supports(Block block) {
        return block == Blocks.DISPENSER || block == Blocks.OBSERVER;
    }

    private static InteractionHand heldHand(Minecraft mc) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (mc.player.getItemInHand(hand).getItem() instanceof BlockItem item && supports(item.getBlock())) {
                return hand;
            }
        }
        return null;
    }

    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level) {
            world = mc.level;
            selection = 0;
        }
        while (CYCLE.consumeClick()) {
            if (mc.player != null && mc.screen == null && heldHand(mc) != null) {
                selection = Screen.hasShiftDown() ? 0 : (selection + 1) % 7;
            }
        }
    }

    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null || !mc.mouseHandler.isMouseGrabbed()
                || event.getScrollDeltaY() == 0 || heldHand(mc) == null) {
            return;
        }
        long window = mc.getWindow().getWindow();
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            selection = Math.floorMod(selection + (event.getScrollDeltaY() > 0 ? 1 : -1), 7);
            event.setCanceled(true);
        }
    }

    private static Preview preview(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.player.isSpectator() || mc.screen != null
                || !OverlayControls.visible(mc) || !(mc.hitResult instanceof BlockHitResult hit)) {
            return null;
        }
        InteractionHand hand = heldHand(mc);
        if (hand == null) {
            return null;
        }
        var stack = mc.player.getItemInHand(hand);
        Block block = ((BlockItem) stack.getItem()).getBlock();
        BlockPlaceContext context = new BlockPlaceContext(mc.player, hand, stack, hit);
        BlockPos pos = context.getClickedPos();
        Direction direction = facing();
        if (direction == null) {
            var state = block.getStateForPlacement(context);
            if (state == null) {
                return null;
            }
            direction = state.getValue(BlockStateProperties.FACING);
        }
        boolean valid = context.canPlace() && mc.level.getWorldBorder().isWithinBounds(pos)
                && pos.getY() >= mc.level.getMinBuildHeight() && pos.getY() < mc.level.getMaxBuildHeight()
                && mc.level.noCollision(new AABB(pos).deflate(0.001));
        return new Preview(pos, direction, block == Blocks.OBSERVER, valid);
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Preview preview = preview(mc);
        if (preview == null) {
            return;
        }
        var camera = event.getCamera().getPosition();
        PoseStack pose = new PoseStack();
        pose.mulPose(event.getModelViewMatrix());
        pose.translate(-camera.x, -camera.y, -camera.z);
        var buffers = mc.renderBuffers().bufferSource();
        var lines = buffers.getBuffer(RenderType.lines());
        float red = preview.valid ? 0.2F : 1;
        float green = preview.valid ? 1 : 0.2F;
        LevelRenderer.renderLineBox(pose, lines, new AABB(preview.pos).inflate(0.003), red, green, 0.3F, 1);
        Vec3 center = Vec3.atCenterOf(preview.pos);
        Direction direction = preview.direction;
        Vec3 forward = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        Vec3 tip = center.add(forward.scale(0.9));
        Vec3 base = center.add(forward.scale(0.65));
        Vec3 side = direction.getAxis() == Direction.Axis.Y ? new Vec3(0.14, 0, 0) : new Vec3(0, 0.14, 0);
        line(pose, lines, center, tip, red, green);
        line(pose, lines, tip, base.add(side), red, green);
        line(pose, lines, tip, base.subtract(side), red, green);
        buffers.endBatch(RenderType.lines());
    }

    private static void line(PoseStack pose, VertexConsumer buffer, Vec3 start, Vec3 end, float red, float green) {
        Vec3 normal = end.subtract(start).normalize();
        for (Vec3 point : new Vec3[]{start, end}) {
            buffer.addVertex(pose.last(), (float) point.x, (float) point.y, (float) point.z)
                    .setColor(red, green, 0.3F, 1)
                    .setNormal(pose.last(), (float) normal.x, (float) normal.y, (float) normal.z);
        }
    }

    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Preview preview = preview(mc);
        if (preview == null) {
            return;
        }
        String text = (preview.observer ? "Observer detecting face: " : "Dispenser opening: ")
                + preview.direction.getName() + (facing() == null ? " (vanilla)" : " (locked)");
        String controls = "Ctrl+Scroll / " + CYCLE.getTranslatedKeyMessage().getString()
                + " cycle | Shift+" + CYCLE.getTranslatedKeyMessage().getString() + " vanilla";
        int width = mc.getWindow().getGuiScaledWidth();
        int y = mc.getWindow().getGuiScaledHeight() - 70;
        event.getGuiGraphics().drawString(mc.font, text, (width - mc.font.width(text)) / 2, y, 0xFFFFFFFF);
        event.getGuiGraphics().drawString(mc.font, controls, (width - mc.font.width(controls)) / 2, y + 11, 0xFFAAAAAA);
    }

    private record Preview(BlockPos pos, Direction direction, boolean observer, boolean valid) {}
}
