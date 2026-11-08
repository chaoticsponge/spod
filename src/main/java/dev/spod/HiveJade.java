package dev.spod;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import snownee.jade.addon.vanilla.BeehiveProvider;
import snownee.jade.api.BlockAccessor;
import snownee.jade.impl.ObjectDataCenter;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.util.ClientProxy;

/** Loaded only when the optional Jade mod is present. */
public final class HiveJade {
    private HiveJade() {}

    public static boolean isConnected() {
        return WailaClientRegistration.instance().isServerConnected();
    }

    public static boolean canRequest(Minecraft mc, BlockPos pos) {
        double range = mc.player.blockInteractionRange() + 21;
        return pos.distSqr(mc.player.blockPosition()) <= range * range;
    }

    private static BlockAccessor accessor(Minecraft mc, BlockPos pos, CompoundTag data) {
        return WailaClientRegistration.instance().blockAccessor()
                .level(mc.level).player(mc.player).serverConnected(true)
                .hit(new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false))
                .blockState(mc.level.getBlockState(pos)).blockEntity(mc.level.getBlockEntity(pos))
                .serverData(data).build();
    }

    public static void request(Minecraft mc, BlockPos pos) {
        ClientProxy.requestBlockData(accessor(mc, pos, new CompoundTag()), List.of(BeehiveProvider.INSTANCE));
    }

    public static boolean receive(CompoundTag tag) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !tag.contains(BeehiveProvider.INSTANCE.getUid().toString())
                || !tag.contains("x", Tag.TAG_INT) || !tag.contains("y", Tag.TAG_INT) || !tag.contains("z", Tag.TAG_INT)) {
            return false;
        }
        BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        if (!mc.level.hasChunkAt(pos) || !(mc.level.getBlockState(pos).getBlock() instanceof BeehiveBlock)) {
            return false;
        }
        BlockAccessor accessor = accessor(mc, pos, tag);
        if (!BuiltInRegistries.BLOCK.getKey(accessor.getBlock()).toString().equals(tag.getString("BlockId"))) {
            return false;
        }
        BeehiveProvider.INSTANCE.decodeFromData(accessor)
                .ifPresent(value -> HiveData.acceptJade(pos, Math.abs((int) value)));
        var current = ObjectDataCenter.get();
        // Keep background hive replies from replacing Jade's normal tooltip data.
        return !(current instanceof BlockAccessor block && block.getPosition().equals(pos));
    }
}
