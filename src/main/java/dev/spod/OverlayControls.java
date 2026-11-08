package dev.spod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class OverlayControls {
    private static final KeyMapping TOGGLE = new KeyMapping("key.spod.toggle_overlays",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories.spod");
    private static boolean enabled = true;

    private OverlayControls() {}

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
    }

    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (TOGGLE.consumeClick()) {
            if (mc.level != null && mc.screen == null) {
                enabled = !enabled;
            }
        }
    }

    public static boolean visible(Minecraft mc) {
        return enabled && !mc.options.hideGui;
    }
}
