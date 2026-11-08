package dev.spod;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class AutoReconnect {
    private static final ReconnectSchedule SCHEDULE = new ReconnectSchedule();
    private static ServerData server;
    private static ClientLevel world;
    private static Screen disconnectScreen;
    private static boolean connecting;

    private AutoReconnect() {}

    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            if (mc.level != world) {
                cancel();
                ServerData current = mc.getCurrentServer();
                if (mc.getSingleplayerServer() == null && current != null && !current.isRealm()) {
                    server = new ServerData(current.name, current.ip, current.type());
                    server.copyFrom(current);
                    world = mc.level;
                }
            }
            return;
        }
        if (server == null) {
            return;
        }
        world = null;
        if (!mc.allowsMultiplayer()) {
            cancel();
            return;
        }
        if (mc.screen instanceof DisconnectedScreen) {
            if (disconnectScreen != mc.screen) {
                disconnectScreen = mc.screen;
                connecting = false;
                SCHEDULE.disconnected(Util.getMillis());
            }
            if (SCHEDULE.takeAttempt(Util.getMillis())) {
                connecting = true;
                disconnectScreen = null;
                ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), mc,
                        ServerAddress.parseString(server.ip), server, false, null);
            }
        } else if (disconnectScreen != null || !connecting
                || mc.screen instanceof JoinMultiplayerScreen || mc.screen instanceof TitleScreen) {
            // Leaving the disconnect screen or canceling the connection cancels all future retries.
            // Login/resource-pack/loading screens are allowed while the reconnect is in progress.
            cancel();
        }
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (server == null || !(event.getScreen() instanceof DisconnectedScreen)) {
            return;
        }
        Screen screen = event.getScreen();
        event.addListener(Button.builder(Component.literal("Stop auto reconnect"), button -> {
            cancel();
            button.active = false;
            button.setMessage(Component.literal("Auto reconnect stopped"));
        }).bounds(screen.width / 2 - 100, screen.height - 30, 200, 20).build());
    }

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (server == null || !SCHEDULE.isWaiting() || event.getScreen() != disconnectScreen) {
            return;
        }
        long seconds = SCHEDULE.remainingSeconds(Util.getMillis());
        String message = "Auto reconnect in " + seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60;
        Screen screen = event.getScreen();
        event.getGuiGraphics().drawCenteredString(Minecraft.getInstance().font, message,
                screen.width / 2, screen.height - 44, 0xFFFFFFFF);
    }

    private static void cancel() {
        server = null;
        world = null;
        disconnectScreen = null;
        connecting = false;
        SCHEDULE.reset();
    }
}
