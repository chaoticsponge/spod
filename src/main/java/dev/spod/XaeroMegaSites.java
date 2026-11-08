package dev.spod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Optional integration: no Xaero classes are loaded when the minimap is absent. */
final class XaeroMegaSites {
    private final Method session;
    private final Constructor<?> waypoint;
    private final Method add;
    private final Method save;
    private final Method visibility;
    private final Object mapVisibility;

    XaeroMegaSites() throws ReflectiveOperationException {
        Class<?> point = Class.forName("xaero.common.minimap.waypoints.Waypoint");
        Class<?> world = Class.forName("xaero.hud.minimap.world.MinimapWorld");
        Class<?> set = Class.forName("xaero.hud.minimap.waypoint.set.WaypointSet");
        Class<?> io = Class.forName("xaero.hud.minimap.world.io.MinimapWorldManagerIO");
        Class<?> visible = Class.forName("xaero.common.minimap.waypoints.WaypointVisibilityType");
        session = Class.forName("xaero.common.XaeroMinimapSession").getMethod("getCurrentSession");
        waypoint = point.getConstructor(int.class, int.class, int.class, String.class, String.class, int.class);
        add = set.getMethod("add", point);
        save = io.getMethod("saveWorld", world);
        visibility = point.getMethod("setVisibility", visible);
        mapVisibility = visible.getField("WORLD_MAP_GLOBAL").get(null);
    }

    boolean mark(BlockPos pos, ResourceKey<Level> dimension, MegaSitePattern pattern) throws ReflectiveOperationException {
        Object current = session.invoke(null);
        if (current == null) return false;
        Object manager = call(current, "getWaypointsManager");
        // Use the player's actual world, never the world being browsed on the map.
        Object world = call(call(manager, "getWorldManager"), "getAutoWorld");
        if (world == null || !dimension.equals(call(world, "getDimId"))) return false;
        for (Object set : (Iterable<?>) call(world, "getIterableWaypointSets")) {
            for (Object point : (Iterable<?>) call(set, "getWaypoints")) {
                if (pattern.name().equals(call(point, "getName"))
                        && Math.abs((int) call(point, "getX") - pos.getX()) <= 16
                        && Math.abs((int) call(point, "getY") - pos.getY()) <= 16
                        && Math.abs((int) call(point, "getZ") - pos.getZ()) <= 16) return true;
            }
        }
        Object set = call(world, "getCurrentWaypointSet");
        if (set == null) return false;
        Object point = waypoint.newInstance(pos.getX(), pos.getY(), pos.getZ(), pattern.name(), pattern.symbol(), pattern.color());
        visibility.invoke(point, mapVisibility);
        add.invoke(set, point);
        save.invoke(call(manager, "getWorldManagerIO"), world);
        return true;
    }

    private static Object call(Object target, String method) throws ReflectiveOperationException {
        return target.getClass().getMethod(method).invoke(target);
    }
}
