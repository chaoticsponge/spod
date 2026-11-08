package dev.spod;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

public final class ScrambleCommand {
    private ScrambleCommand() {}

    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("scramble").executes(context -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.getConnection() == null) {
                context.getSource().sendFailure(Component.literal("Join a world before using /scramble."));
                return 0;
            }
            var puzzle = ScrambleWords.next(ThreadLocalRandom.current());
            // Use ordinary player chat so the puzzle is sent as the local player, including chat signing.
            mc.getConnection().sendChat("Unscramble: " + puzzle.jumbled());
            return 1;
        }));
    }
}
