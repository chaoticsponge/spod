package dev.spod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.LoggerFactory;

final class MegaSiteFinder {
    private static ClientLevel world;
    private static final Map<Block, List<MegaSitePattern>> patterns = new HashMap<>();
    private static int scanIndex;
    private static final Set<BlockPos> marked = new HashSet<>();
    private static XaeroMegaSites xaero;
    private static boolean unavailable;

    private MegaSiteFinder() {}

    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level) {
            world = mc.level;
            marked.clear();
            scanIndex = 0;
        }
        if (world == null || mc.player == null || mc.isPaused() || unavailable
                || !ModList.get().isLoaded("xaerominimap") || !ModList.get().isLoaded("mega_showdown")) return;
        try {
            if (xaero == null) {
                xaero = new XaeroMegaSites();
                for (MegaSitePattern pattern : MegaSitePattern.load()) {
                    BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(pattern.anchor())).ifPresent(block ->
                            patterns.computeIfAbsent(block, ignored -> new ArrayList<>()).add(pattern));
                }
            }
            if (patterns.isEmpty()) return;
            int radius = Math.min(32, mc.options.renderDistance().get());
            int width = radius * 2 + 1;
            var playerChunk = mc.player.chunkPosition();
            // Bound background work; skip entire sections using their block palettes.
            for (int count = 0; count < 8; count++) {
                int index = scanIndex++ % (width * width);
                if (scanIndex >= width * width) scanIndex = 0;
                scanChunk(playerChunk.x + index % width - radius, playerChunk.z + index / width - radius);
            }
        } catch (ReflectiveOperationException | IOException | LinkageError error) {
            unavailable = true;
            LoggerFactory.getLogger(SpodMod.MOD_ID).warn("Mega Showdown waypoint integration disabled", error);
            mc.player.displayClientMessage(Component.literal("Mega Showdown markers unavailable: incompatible Xaero API or waypoint save failure. See latest.log."), false);
        }
    }

    private static void scanChunk(int chunkX, int chunkZ) throws ReflectiveOperationException {
        var chunk = world.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (chunk == null) return;
        var sections = chunk.getSections();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            var section = sections[sectionIndex];
            if (section.hasOnlyAir() || !section.maybeHas(state -> patterns.containsKey(state.getBlock()))) continue;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        var candidates = patterns.get(section.getBlockState(x, y, z).getBlock());
                        if (candidates == null) continue;
                        BlockPos pos = new BlockPos(chunkX * 16 + x,
                                world.getSectionYFromSectionIndex(sectionIndex) * 16 + y, chunkZ * 16 + z);
                        if (marked.contains(pos)) continue;
                        for (MegaSitePattern pattern : candidates) {
                            if (pattern.matches(pos, nearby -> !world.hasChunkAt(nearby) ? ""
                                    : BuiltInRegistries.BLOCK.getKey(world.getBlockState(nearby).getBlock()).toString())
                                    && xaero.mark(pos, world.dimension(), pattern)) {
                                marked.add(pos);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

}
