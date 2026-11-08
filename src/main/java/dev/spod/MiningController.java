package dev.spod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.Tags;

final class MiningController {
    private enum Tool { HAMMER, EXCAVATOR, PICKAXE }
    private static ClientLevel world;
    private static Tool warned;

    private MiningController() {}

    static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mining")
                .executes(context -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (world != null) {
                        stop();
                        context.getSource().sendSuccess(() -> Component.literal("Mining tool switching disabled."), false);
                    } else if (mc.level == null || mc.player == null || !mc.player.isAlive()) {
                        context.getSource().sendFailure(Component.literal("Join a world and respawn before using /mining."));
                        return 0;
                    } else {
                        ClickController.stop();
                        MiningFloor.reset();
                        world = mc.level;
                        warned = null;
                        select(mc, Tool.HAMMER, null);
                        context.getSource().sendSuccess(() -> Component.literal(
                                "Mining enabled: automatic tools and 3x3 floor filling from hotbar stone blocks. Mine manually; /mining stop disables."), false);
                    }
                    return 1;
                })
                .then(Commands.literal("stop").executes(context -> {
                    stop();
                    context.getSource().sendSuccess(() -> Component.literal("Mining tool switching disabled."), false);
                    return 1;
                })));
    }

    static void onTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (world != null && (mc.level != world || mc.player == null || !mc.player.isAlive())) stop();
        if (ready(mc)) selectTarget(mc);
    }

    static void onInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        // NeoForge fires this before both initial and continuing block destruction.
        if (event.isAttack() && ready(mc) && mc.hitResult != null
                && mc.hitResult.getType() == HitResult.Type.BLOCK && !selectTarget(mc)) {
            event.setCanceled(true);
            event.setSwingHand(false);
            mc.gameMode.stopDestroyBlock();
        }
    }

    static void onFloorTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (ready(mc)) MiningFloor.tick(mc);
    }

    static void stop() {
        world = null;
        warned = null;
        MiningFloor.reset();
    }

    private static boolean ready(Minecraft mc) {
        return world != null && mc.level == world && mc.player != null && mc.player.isAlive()
                && mc.gameMode != null && mc.screen == null && mc.getOverlay() == null && !mc.isPaused()
                && mc.isWindowActive() && mc.mouseHandler.isMouseGrabbed() && !mc.player.isUsingItem();
    }

    private static boolean selectTarget(Minecraft mc) {
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return true;
        BlockState state = mc.level.getBlockState(hit.getBlockPos());
        if (state.isAir()) return true;
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        // Some modded ores omit the common ore tag (including Mega Showdown ores).
        Tool tool = state.is(Tags.Blocks.ORES) || path.endsWith("_ore") || path.startsWith("ore_")
                ? Tool.PICKAXE : state.is(BlockTags.MINEABLE_WITH_SHOVEL) ? Tool.EXCAVATOR : Tool.HAMMER;
        return select(mc, tool, state);
    }

    private static boolean select(Minecraft mc, Tool tool, BlockState state) {
        var inventory = mc.player.getInventory();
        int chosen = -1;
        int best = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || kind(stack) != tool || stack.isDamageableItem()
                    && stack.getMaxDamage() - stack.getDamageValue() <= 10) continue;
            if (state != null && state.requiresCorrectToolForDrops() && !stack.isCorrectToolForDrops(state)) continue;
            int score = slot == inventory.selected ? 1 : 0;
            if (tool == Tool.HAMMER && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()
                    .equals("onlyhammersandexcavators:obsidian_hammer")) score += 2;
            if (score > best) {
                chosen = slot;
                best = score;
            }
        }
        if (chosen < 0) {
            if (warned != tool) {
                mc.player.displayClientMessage(Component.literal("Mining paused: need a suitable "
                        + tool.name().toLowerCase(java.util.Locale.ROOT)
                        + " in the hotbar with more than 10 durability."), true);
                warned = tool;
            }
            return false;
        }
        warned = null;
        if (chosen != inventory.selected) {
            mc.gameMode.stopDestroyBlock();
            inventory.selected = chosen;
        }
        return true;
    }

    private static Tool kind(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        // Hammers can extend PickaxeItem; classify them first to protect ores from area mining.
        if (path.endsWith("_hammer") || path.equals("hammer")) return Tool.HAMMER;
        if (path.endsWith("_excavator") || path.equals("excavator")) return Tool.EXCAVATOR;
        return stack.getItem() instanceof PickaxeItem ? Tool.PICKAXE : null;
    }
}
