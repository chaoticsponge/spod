package dev.spod;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

/** Moves one hotbar item at a time without changing the selected sword slot. */
final class MendingSession {
    enum Result { READY, WAIT, COMPLETE, INVENTORY_CHANGED }

    private int repairSlot = -1;
    private int nextSlot;
    private int settleTicks;
    private Item repairItem;
    private ItemStack displaced = ItemStack.EMPTY;

    boolean ownsSlot(int slot) {
        return slot == repairSlot;
    }

    Result tick(Minecraft mc, Runnable release) {
        var player = mc.player;
        if (mc.gameMode == null || player.containerMenu != player.inventoryMenu
                || !player.inventoryMenu.getCarried().isEmpty()) {
            return Result.INVENTORY_CHANGED;
        }
        if (settleTicks > 0) {
            settleTicks--;
            return Result.WAIT;
        }
        var inventory = player.getInventory();
        if (repairSlot >= 0) {
            ItemStack offhand = player.getOffhandItem();
            if (inventory.selected == repairSlot || offhand.isEmpty() || offhand.getItem() != repairItem
                    || !ItemStack.matches(inventory.getItem(repairSlot), displaced)) {
                return Result.INVENTORY_CHANGED;
            }
            if (offhand.isDamaged()) {
                return Result.READY;
            }
            swap(mc, repairSlot, release);
            repairSlot = -1;
            repairItem = null;
            displaced = ItemStack.EMPTY;
            return Result.WAIT;
        }
        for (int offset = 0; offset < 9; offset++) {
            int slot = (nextSlot + offset) % 9;
            ItemStack candidate = inventory.getItem(slot);
            if (slot != inventory.selected && needsMending(mc, candidate)) {
                repairSlot = slot;
                nextSlot = (slot + 1) % 9;
                repairItem = candidate.getItem();
                displaced = player.getOffhandItem().copy();
                swap(mc, slot, release);
                return Result.WAIT;
            }
        }
        return Result.COMPLETE;
    }

    private static boolean needsMending(Minecraft mc, ItemStack stack) {
        return stack.isDamageableItem() && stack.isDamaged()
                && stack.getAllEnchantments(mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT))
                        .keySet().stream().anyMatch(holder -> holder.is(Enchantments.MENDING));
    }

    private void swap(Minecraft mc, int hotbarSlot, Runnable release) {
        release.run();
        // Player-inventory slot 45 is the offhand; SWAP's button selects the hotbar slot.
        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, 45,
                hotbarSlot, ClickType.SWAP, mc.player);
        settleTicks = 4;
    }
}
