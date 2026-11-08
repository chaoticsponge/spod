package dev.spod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import dev.spod.mixin.CarriedItemSync;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class MiningFloor {
    private static final Set<String> MATERIALS = Set.of(
            "stone", "cobblestone", "deepslate", "cobbled_deepslate", "tuff",
            "granite", "diorite", "andesite", "calcite", "blackstone", "basalt",
            "smooth_basalt", "netherrack", "end_stone");
    private static final Map<BlockPos, Long> attempted = new HashMap<>();
    private static int cooldown;
    private static boolean warned;

    private MiningFloor() {}

    static void reset() {
        attempted.clear();
        cooldown = 0;
        warned = false;
    }

    static List<BlockPos> targets(BlockPos feet, Direction forward) {
        var result = new ArrayList<BlockPos>(9);
        // Fill the walking line first, then widen it; never place behind or under the player.
        for (int side : new int[]{0, -1, 1}) {
            for (int ahead = 1; ahead <= 3; ahead++) {
                result.add(feet.below().relative(forward, ahead).relative(forward.getClockWise(), side));
            }
        }
        return result;
    }

    static void tick(Minecraft mc) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        var player = mc.player;
        // Do not build while falling, flying, swimming, using a menu, or standing on steps/slabs.
        double feetY = player.getBoundingBox().minY;
        if (!player.onGround() || player.isSpectator() || player.getAbilities().flying
                || !player.getAbilities().mayBuild || player.isInWater() || player.isInLava()
                || Math.abs(feetY - Math.rint(feetY)) > 0.05 || mc.options.keyUse.isDown()
                || player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty()) return;
        long now = mc.level.getGameTime();
        attempted.entrySet().removeIf(entry -> entry.getValue() <= now);
        int materialSlot = findMaterial(mc);
        for (BlockPos target : targets(BlockPos.containing(player.getX(), feetY, player.getZ()), player.getDirection())) {
            if (!mc.level.hasChunkAt(target) || !mc.level.getBlockState(target).canBeReplaced()
                    || attempted.containsKey(target)) continue;
            BlockHitResult hit = placementFace(mc, target);
            if (hit == null) continue;
            if (materialSlot < 0) {
                if (!warned) player.displayClientMessage(Component.literal(
                        "Floor filling needs common stone blocks in your hotbar (stone, cobblestone, deepslate, tuff, etc.)."), true);
                warned = true;
                return;
            }
            warned = false;
            var inventory = player.getInventory();
            int originalSlot = inventory.selected;
            try {
                inventory.selected = materialSlot;
                var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, player.getMainHandItem(), hit);
                if (!context.getClickedPos().equals(target) || !context.canPlace()) continue;
                // Vanilla placement handles entity collisions, build limits and server permissions.
                attempted.put(target, now + 20);
                mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
                cooldown = 1;
                return;
            } finally {
                inventory.selected = originalSlot;
                // Restore the server's held slot too, before the next manual mining action.
                ((CarriedItemSync) mc.gameMode).spod$syncCarriedItem();
            }
        }
    }

    private static int findMaterial(Minecraft mc) {
        for (int slot = 0; slot < 9; slot++) {
            var stack = mc.player.getInventory().getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) continue;
            var id = BuiltInRegistries.BLOCK.getKey(item.getBlock());
            if (id.getNamespace().equals("minecraft") && MATERIALS.contains(id.getPath())) return slot;
        }
        return -1;
    }

    private static BlockHitResult placementFace(Minecraft mc, BlockPos target) {
        Vec3 eye = mc.player.getEyePosition();
        double reach = mc.player.blockInteractionRange();
        for (Direction face : Direction.values()) {
            BlockPos support = target.relative(face.getOpposite());
            if (!mc.level.hasChunkAt(support)) continue;
            var state = mc.level.getBlockState(support);
            if (state.canBeReplaced() || state.hasBlockEntity() || !state.isCollisionShapeFullBlock(mc.level, support)
                    || state.getMenuProvider(mc.level, support) != null) continue;
            Vec3 point = Vec3.atCenterOf(support).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            if (eye.distanceToSqr(point) <= reach * reach) return new BlockHitResult(point, face, support, false);
        }
        return null;
    }
}
