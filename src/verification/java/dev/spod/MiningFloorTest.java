package dev.spod;

import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class MiningFloorTest {
    public static void main(String[] args) {
        for (BlockPos feet : new BlockPos[]{new BlockPos(0, 64, 0), new BlockPos(-137, -42, -351)}) {
            for (Direction forward : Direction.Plane.HORIZONTAL) {
                var targets = MiningFloor.targets(feet, forward);
                check(targets.size() == 9 && new HashSet<>(targets).size() == 9, "Exactly nine distinct floor cells");
                check(!targets.contains(feet.below()), "Do not fill under the player");
                for (int i = 0; i < 3; i++) {
                    check(targets.get(i).equals(feet.below().relative(forward, i + 1)), "Prioritize the walking line nearest first");
                }
                for (BlockPos pos : targets) {
                    int x = pos.getX() - feet.getX();
                    int z = pos.getZ() - feet.getZ();
                    int ahead = x * forward.getStepX() + z * forward.getStepZ();
                    Direction right = forward.getClockWise();
                    int side = x * right.getStepX() + z * right.getStepZ();
                    check(pos.getY() == feet.getY() - 1 && ahead >= 1 && ahead <= 3 && Math.abs(side) <= 1,
                            "Floor must be three wide, three ahead, one below feet in every facing");
                }
            }
        }
        System.out.println("Mining floor checks passed: 3x3 bounds, all facings, negative coordinates and walking-line priority.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
