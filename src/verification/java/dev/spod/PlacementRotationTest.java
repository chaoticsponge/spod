package dev.spod;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class PlacementRotationTest {
    public static void main(String[] args) {
        for (Direction facing : Direction.values()) {
            for (boolean observer : new boolean[]{false, true}) {
                PlacementRotation rotation = PlacementRotation.forFacing(facing, observer);
                Vec3 look = Vec3.directionFromRotation(rotation.pitch(), rotation.yaw());
                Direction nearest = Direction.getNearest(look.x, look.y, look.z);
                Direction actual = observer ? nearest : nearest.getOpposite();
                if (actual != facing) {
                    throw new AssertionError((observer ? "Observer" : "Dispenser") + " facing " + facing + " became " + actual);
                }
            }
        }
        System.out.println("Placement checks passed: all six directions for dispenser openings and observer detecting faces.");
    }
}
