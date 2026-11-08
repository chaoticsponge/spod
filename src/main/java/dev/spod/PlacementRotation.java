package dev.spod;

import net.minecraft.core.Direction;

/** Vanilla observers detect along the look direction; dispensers face the opposite way. */
public record PlacementRotation(float yaw, float pitch) {
    public static PlacementRotation forFacing(Direction facing, boolean observer) {
        Direction look = observer ? facing : facing.getOpposite();
        return switch (look) {
            case UP -> new PlacementRotation(0, -90);
            case DOWN -> new PlacementRotation(0, 90);
            default -> new PlacementRotation(look.toYRot(), 0);
        };
    }
}
