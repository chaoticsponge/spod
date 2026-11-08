package dev.spod;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

public final class ClickController {
    private static final int MIN_DURABILITY = 10;
    private static KeyMapping key;
    private static ClickCycle cycle;
    private static Item selectedItem;
    private static ClientLevel world;
    private static long elapsedTicks;
    private static boolean pressed;
    private static MendingSession mending;

    private ClickController() {}

    public static void registerCommands(RegisterClientCommandsEvent event) {
        var root = Commands.literal("click")
                .then(Commands.literal("mending").executes(context -> {
                    int result = start(context.getSource(), "left", ClickCycle.seconds(0.1, 1), Items.IRON_SWORD);
                    if (result == 1) {
                        mending = new MendingSession();
                        context.getSource().sendSuccess(() -> Component.literal(
                                "Mending damaged hotbar items in the offhand. Collect XP to repair them; /click stop cancels."), false);
                    }
                    return result;
                }))
                .then(Commands.literal("stop").executes(context -> {
                    stop();
                    context.getSource().sendSuccess(() -> Component.literal("Auto-click stopped."), false);
                    return 1;
                }));
        for (String button : new String[]{"left", "right"}) {
            root.then(Commands.literal(button)
                    .executes(context -> start(context.getSource(), button, null, null))
                    .then(timing(button, false))
                    .then(Commands.argument("tool", ItemArgument.item(event.getBuildContext()))
                            .executes(context -> start(context.getSource(), button, null,
                                    ItemArgument.getItem(context, "tool").getItem()))
                            .then(timing(button, true))));
        }
        event.getDispatcher().register(root);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Double> timing(String button, boolean hasTool) {
        return Commands.argument("holdSeconds", DoubleArgumentType.doubleArg(ClickCycle.MIN_SECONDS, ClickCycle.MAX_SECONDS))
                .then(Commands.argument("intervalSeconds", DoubleArgumentType.doubleArg(ClickCycle.MIN_SECONDS, ClickCycle.MAX_SECONDS))
                        .executes(context -> start(context.getSource(), button, ClickCycle.seconds(
                                DoubleArgumentType.getDouble(context, "holdSeconds"),
                                DoubleArgumentType.getDouble(context, "intervalSeconds")),
                                hasTool ? ItemArgument.getItem(context, "tool").getItem() : null)));
    }

    private static int start(CommandSourceStack source, String button, ClickCycle schedule, Item requestedItem) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.player.isAlive()) {
            source.sendFailure(Component.literal("Join a world and respawn before using /click."));
            return 0;
        }
        stop();
        MiningController.stop();
        ItemStack offhand = mc.player.getOffhandItem();
        if (button.equals("right") && isFragile(offhand)) {
            source.sendFailure(durabilityMessage(offhand));
            return 0;
        }
        selectedItem = requestedItem;
        if (!selectTool(mc, false)) {
            source.sendFailure(missingToolMessage());
            stop();
            return 0;
        }
        key = button.equals("left") ? mc.options.keyAttack : mc.options.keyUse;
        world = mc.level;
        cycle = schedule;
        String timing = schedule == null ? " continuously" : " for " + schedule.holdTicks() / 20.0
                + "s, releasing for " + schedule.intervalTicks() / 20.0 + "s, repeating";
        source.sendSuccess(() -> Component.literal("Holding " + button + timing + ". Use /click stop to stop."), false);
        return 1;
    }

    public static void onTick(ClientTickEvent.Pre event) {
        if (key == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != world || mc.player == null || !mc.player.isAlive()) {
            stop();
            return;
        }
        ItemStack offhand = mc.player.getOffhandItem();
        if (key == mc.options.keyUse && isFragile(offhand)) {
            stop();
            mc.player.displayClientMessage(durabilityMessage(offhand), false);
            return;
        }
        // Suspend automation in chat, menus and unfocused windows without consuming cycle time.
        if (mc.screen != null || mc.getOverlay() != null || mc.isPaused() || !mc.isWindowActive()
                || !mc.mouseHandler.isMouseGrabbed()) {
            release();
            return;
        }
        if (!selectTool(mc, true)) {
            Component message = missingToolMessage();
            stop();
            mc.player.displayClientMessage(message, false);
            return;
        }
        if (mending != null) {
            // Never attack while moving items, even when a swap falls in a hold phase.
            MendingSession.Result result = mending.tick(mc, ClickController::release);
            if (result != MendingSession.Result.READY) {
                release();
                elapsedTicks = 0;
                if (result != MendingSession.Result.WAIT) {
                    stop();
                    mc.player.displayClientMessage(Component.literal(result == MendingSession.Result.COMPLETE
                            ? "Mending finished: no damaged Mending items remain in the hotbar."
                            : "Mending stopped: inventory changed; check your offhand and hotbar."), false);
                }
                return;
            }
        }
        boolean down = cycle == null || cycle.isHolding(elapsedTicks);
        if (down && !pressed) {
            // Holding alone does not initiate entity attacks; queue one real press per hold phase.
            KeyMapping.click(key.getKey());
        }
        if (!down && pressed) {
            release();
        }
        key.setDown(down);
        pressed = down;
        elapsedTicks++;
    }

    private static boolean selectTool(Minecraft mc, boolean notify) {
        ItemStack main = mc.player.getMainHandItem();
        if (selectedItem == null && main.isDamageableItem()) {
            selectedItem = main.getItem();
        }
        if (selectedItem == null || matchesTool(main) && !isFragile(main)
                && (mending == null || !mending.ownsSlot(mc.player.getInventory().selected))) {
            return true;
        }
        var inventory = mc.player.getInventory();
        for (int offset = 1; offset <= 9; offset++) {
            int slot = (inventory.selected + offset) % 9;
            ItemStack candidate = inventory.getItem(slot);
            if (matchesTool(candidate) && !isFragile(candidate) && (mending == null || !mending.ownsSlot(slot))) {
                // Release using the old slot before selecting the replacement. Vanilla syncs the slot before acting.
                release();
                inventory.selected = slot;
                if (notify) {
                    mc.player.displayClientMessage(Component.literal("Auto-click switched to hotbar slot " + (slot + 1) + ": ")
                            .append(candidate.getHoverName()), false);
                }
                return true;
            }
        }
        return false;
    }

    private static Component missingToolMessage() {
        return Component.literal("Auto-click stopped: no usable replacement for ").append(new ItemStack(selectedItem).getHoverName())
                .append(" in the hotbar (same tool type, any material; need more than " + MIN_DURABILITY + " durability).");
    }

    private static boolean matchesTool(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item == selectedItem
                || selectedItem instanceof HoeItem && item instanceof HoeItem
                || selectedItem instanceof PickaxeItem && item instanceof PickaxeItem
                || selectedItem instanceof AxeItem && item instanceof AxeItem
                || selectedItem instanceof ShovelItem && item instanceof ShovelItem
                || selectedItem instanceof SwordItem && item instanceof SwordItem;
    }

    private static boolean isFragile(ItemStack stack) {
        return stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= MIN_DURABILITY;
    }

    private static Component durabilityMessage(ItemStack stack) {
        return Component.literal("Auto-click stopped: ").append(stack.getHoverName())
                .append(" has " + (stack.getMaxDamage() - stack.getDamageValue())
                        + " durability remaining (reserve " + MIN_DURABILITY + ").");
    }

    private static void release() {
        if (key != null && pressed) {
            key.setDown(false);
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == world && mc.gameMode != null && mc.player != null) {
                if (key == mc.options.keyAttack) {
                    mc.gameMode.stopDestroyBlock();
                } else if (mc.player.isUsingItem()) {
                    mc.gameMode.releaseUsingItem(mc.player);
                }
            }
        }
        pressed = false;
    }

    static void stop() {
        release();
        key = null;
        cycle = null;
        selectedItem = null;
        world = null;
        elapsedTicks = 0;
        mending = null;
    }
}
