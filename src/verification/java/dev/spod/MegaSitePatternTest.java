package dev.spod;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;

public final class MegaSitePatternTest {
    public static void main(String[] args) throws Exception {
        var patterns = MegaSitePattern.load();
        check(patterns.stream().map(MegaSitePattern::name).collect(Collectors.toSet()).equals(Set.of(
                "Mega Site", "Megaroid (Keystone)", "Observatory", "Archaeological Site", "Wishing Weald")),
                "All five supported structures need fingerprints");
        BlockPos origin = new BlockPos(-137, -42, 351);
        for (var pattern : patterns) {
            check(pattern.samples().size() == 16, "Each signature needs sixteen surrounding blocks");
            check(!pattern.matches(origin, pos -> null), "Unloaded terrain must not match");
            check(!pattern.matches(origin, pos -> pattern.anchor()), "Isolated or repeated anchor blocks must not match");
            for (int mirror : new int[]{1, -1}) {
                for (int turn = 0; turn < 4; turn++) {
                    Map<BlockPos, String> world = new HashMap<>();
                    for (var sample : pattern.samples()) {
                        int x = sample.x() * mirror;
                        int z = sample.z();
                        BlockPos relative = switch (turn) {
                            case 0 -> new BlockPos(x, sample.y(), z);
                            case 1 -> new BlockPos(-z, sample.y(), x);
                            case 2 -> new BlockPos(-x, sample.y(), -z);
                            default -> new BlockPos(z, sample.y(), -x);
                        };
                        world.put(origin.offset(relative), sample.block());
                    }
                    check(world.size() == 16, "Fingerprint sample locations must be unique");
                    check(pattern.matches(origin, world::get), pattern.name() + " must match rotated/mirrored terrain");
                    var positions = world.keySet().stream().toList();
                    world.remove(positions.get(0));
                    world.remove(positions.get(1));
                    check(pattern.matches(origin, world::get), "Two mined blocks may be tolerated");
                    world.remove(positions.get(2));
                    check(!pattern.matches(origin, world::get), "Incomplete signatures must not create waypoints");
                }
            }
        }
        System.out.println("POI checks passed: five structures, rotations, mirrors, negative coordinates, missing terrain and altered blocks.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
