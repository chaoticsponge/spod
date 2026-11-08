package dev.spod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class HiveData {
    public static final int RANGE = 32;
    private static final boolean HAS_JADE = ModList.get().isLoaded("jade");
    private static final List<BlockPos> POSITIONS = new ArrayList<>();
    private static final Map<BlockPos, Count> COUNTS = new HashMap<>();
    private static final Map<BlockPos, Long> QUERIED = new HashMap<>();
    private static ClientLevel level;
    private static long tick;
    private static long pendingUntil;

    private HiveData() {}

    public static List<BlockPos> positions() {
        return POSITIONS;
    }

    public static int count(BlockPos pos) {
        Count count = COUNTS.get(pos);
        return count == null || tick - count.tick > 100 ? -1 : count.bees;
    }

    public static void acceptJade(BlockPos pos, int bees) {
        if (Minecraft.getInstance().level == level && POSITIONS.contains(pos) && bees >= 0) {
            COUNTS.put(pos, new Count(bees, tick));
        }
    }

    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (level != mc.level || !OverlayControls.visible(mc)) {
            level = mc.level;
            POSITIONS.clear();
            COUNTS.clear();
            QUERIED.clear();
            tick = 0;
            pendingUntil = 0;
        }
        if (level == null || mc.player == null || mc.isPaused() || !OverlayControls.visible(mc)) {
            return;
        }
        tick++;
        if (tick % 10 == 1) {
            scan(mc);
            if (mc.getSingleplayerServer() != null) {
                readIntegratedServer(mc);
            }
        }
        if (mc.getSingleplayerServer() == null && mc.getConnection() != null
                && tick >= pendingUntil && tick % 5 == 0) {
            boolean jade = HAS_JADE && HiveJade.isConnected();
            if (jade || mc.player.hasPermissions(2)) {
                queryServer(mc, jade);
            }
        }
    }

    private static void scan(Minecraft mc) {
        POSITIONS.clear();
        var camera = mc.gameRenderer.getMainCamera().getPosition();
        int minX = (int) Math.floor((camera.x - RANGE) / 16);
        int maxX = (int) Math.floor((camera.x + RANGE) / 16);
        int minZ = (int) Math.floor((camera.z - RANGE) / 16);
        int maxZ = (int) Math.floor((camera.z + RANGE) / 16);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                var chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }
                for (var entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof BeehiveBlockEntity && entity.getBlockPos().distToCenterSqr(camera) <= RANGE * RANGE) {
                        POSITIONS.add(entity.getBlockPos());
                    }
                }
            }
        }
        COUNTS.keySet().retainAll(POSITIONS);
        QUERIED.keySet().retainAll(POSITIONS);
    }

    private static void readIntegratedServer(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        var expectedLevel = level;
        var dimension = level.dimension();
        var positions = List.copyOf(POSITIONS);
        server.execute(() -> {
            var world = server.getLevel(dimension);
            Map<BlockPos, Integer> snapshot = new HashMap<>();
            if (world != null) {
                for (BlockPos pos : positions) {
                    if (world.hasChunkAt(pos) && world.getBlockEntity(pos) instanceof BeehiveBlockEntity hive) {
                        snapshot.put(pos, hive.getOccupantCount());
                    }
                }
            }
            mc.execute(() -> {
                if (mc.level == expectedLevel && OverlayControls.visible(mc)) {
                    snapshot.forEach((pos, bees) -> COUNTS.put(pos, new Count(bees, tick)));
                }
            });
        });
    }

    private static void queryServer(Minecraft mc, boolean jade) {
        BlockPos pos = POSITIONS.stream()
                .filter(p -> tick - QUERIED.getOrDefault(p, -20L) >= 20)
                .filter(p -> !jade || HiveJade.canRequest(mc, p))
                .min(Comparator.comparingLong(p -> QUERIED.getOrDefault(p, -20L)))
                .orElse(null);
        if (pos == null) {
            return;
        }
        var expectedLevel = level;
        QUERIED.put(pos, tick);
        pendingUntil = tick + (jade ? 5 : 40);
        if (jade) {
            HiveJade.request(mc, pos);
            return;
        }
        // Vanilla allows only one outstanding debug query and requires operator permission.
        mc.getConnection().getDebugQueryHandler().queryBlockEntityTag(pos, tag -> {
            if (mc.level == expectedLevel && OverlayControls.visible(mc)) {
                pendingUntil = tick + 5;
                if (tag != null && tag.contains("bees", Tag.TAG_LIST) && POSITIONS.contains(pos)) {
                    COUNTS.put(pos, new Count(tag.getList("bees", Tag.TAG_COMPOUND).size(), tick));
                } else {
                    COUNTS.remove(pos);
                }
            }
        });
    }

    private record Count(int bees, long tick) {}
}
