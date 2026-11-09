package dev.spod;

import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class MiningTorches {
    private static int cooldown;

    private MiningTorches() {}

    static void reset() {
        cooldown = 0;
    }

    static void tick(Minecraft mc) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        cooldown = 9;
        var player = mc.player;
        var stack = player.getOffhandItem();
        var torch = stack.is(Items.TORCH) ? Blocks.TORCH : stack.is(Items.SOUL_TORCH) ? Blocks.SOUL_TORCH : null;
        if (torch == null || !player.onGround() || player.isSpectator() || player.getAbilities().flying
                || !player.getAbilities().mayBuild || player.isInWater() || player.isInLava()
                || mc.options.keyUse.isDown() || player.containerMenu != player.inventoryMenu
                || !player.inventoryMenu.getCarried().isEmpty()) return;
        int limit = mc.level.dimensionType().monsterSpawnBlockLightLimit();
        int emission = torch.defaultBlockState().getLightEmission(mc.level, player.blockPosition());
        // Some dimensions permit hostile spawning even at torch brightness; avoid wasting torches there.
        if (emission <= limit) return;
        var floors = MiningFloor.targets(player.blockPosition(), player.getDirection());
        var dark = new ArrayList<BlockPos>();
        for (BlockPos floor : floors) {
            if (SpawnOverlay.isSpawnableSurface(mc.level, floor)) dark.add(floor);
        }
        if (dark.isEmpty()) return;
        var candidates = new ArrayList<>(floors);
        candidates.add(player.blockPosition().below());
        BlockHitResult bestHit = null;
        int bestScore = 0;
        double bestDistance = -1;
        for (BlockPos floor : candidates) {
            BlockPos target = floor.above();
            if (!mc.level.hasChunkAt(floor) || !mc.level.getBlockState(target).isAir()
                    || !torch.defaultBlockState().canSurvive(mc.level, target)) continue;
            var support = mc.level.getBlockState(floor);
            if (support.hasBlockEntity() || support.getMenuProvider(mc.level, floor) != null) continue;
            Vec3 point = Vec3.atBottomCenterOf(target);
            double distance = player.getEyePosition().distanceToSqr(point);
            if (distance > player.blockInteractionRange() * player.blockInteractionRange()) continue;
            // Only click an exposed top face, rather than placing through cave walls.
            BlockHitResult hit = mc.level.clip(new ClipContext(player.getEyePosition(), point.add(0, -0.01, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(floor)
                    || hit.getDirection() != Direction.UP) continue;
            var context = new BlockPlaceContext(player, InteractionHand.OFF_HAND, stack, hit);
            if (!context.getClickedPos().equals(target) || !context.canPlace()) continue;
            int score = 0;
            for (BlockPos unsafe : dark) {
                if (covers(target.distManhattan(unsafe.above()), emission, limit)) score++;
            }
            // Prefer the furthest reachable placement when coverage is equal, spacing torches along the tunnel.
            if (score > bestScore || score > 0 && score == bestScore && distance > bestDistance) {
                bestHit = hit;
                bestScore = score;
                bestDistance = distance;
            }
        }
        if (bestHit != null) {
            mc.gameMode.useItemOn(player, InteractionHand.OFF_HAND, bestHit);
            // Let server inventory/lighting updates arrive before evaluating another placement.
            cooldown = 39;
        }
    }

    // An unobstructed light estimate ranks placements only; actual light decides whether more are needed.
    static boolean covers(int distance, int emission, int spawnLimit) {
        return emission - distance > spawnLimit;
    }
}
