package dev.spod;

import dev.spod.mixin.CarriedItemSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

final class MiningCombat {
    private static Mob target;
    private static LocalPlayer owner;
    private static int originalSlot = -1;
    private static int swordSlot = -1;
    private static int equippedTicks;
    private static boolean warned;

    private MiningCombat() {}

    static boolean ownsTool() {
        return swordSlot >= 0;
    }

    static boolean prepare(Minecraft mc) {
        var player = mc.player;
        double reach = player.entityInteractionRange();
        Mob nearest = null;
        double distance = Double.MAX_VALUE;
        if (!player.isSpectator()) {
            for (Mob mob : mc.level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(reach),
                    mob -> eligible(mc, mob))) {
                double candidateDistance = mob.distanceToSqr(player);
                if (candidateDistance < distance) {
                    nearest = mob;
                    distance = candidateDistance;
                }
            }
        }
        if (nearest == null) {
            stop(mc);
            warned = false;
            return false;
        }
        var inventory = player.getInventory();
        int chosen = usable(inventory.getSelected()) ? inventory.selected : -1;
        for (int slot = 0; chosen < 0 && slot < 9; slot++) {
            if (usable(inventory.getItem(slot))) chosen = slot;
        }
        if (chosen < 0) {
            stop(mc);
            if (!warned) player.displayClientMessage(Component.literal(
                    "Mining defense needs a hotbar sword with more than 10 durability."), true);
            warned = true;
            return false;
        }
        warned = false;
        if (originalSlot < 0) {
            originalSlot = inventory.selected;
            owner = player;
        }
        if (chosen != swordSlot || inventory.selected != chosen) {
            mc.gameMode.stopDestroyBlock();
            inventory.selected = chosen;
            ((CarriedItemSync) mc.gameMode).spod$syncCarriedItem();
            swordSlot = chosen;
            equippedTicks = 0;
        } else {
            equippedTicks++;
        }
        target = nearest;
        return true;
    }

    static void attack(Minecraft mc) {
        if (target == null || equippedTicks < 2 || mc.player.getInventory().selected != swordSlot
                || !usable(mc.player.getMainHandItem()) || !eligible(mc, target)
                || mc.player.getAttackStrengthScale(0) < 1) return;
        // Use normal attacks and the game's actual sword cooldown, including attribute modifiers.
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    static void stop(Minecraft mc) {
        if (originalSlot >= 0 && mc.player != null && mc.player == owner && mc.gameMode != null
                && mc.player.getInventory().selected == swordSlot) {
            mc.player.getInventory().selected = originalSlot;
            ((CarriedItemSync) mc.gameMode).spod$syncCarriedItem();
        }
        target = null;
        owner = null;
        originalSlot = -1;
        swordSlot = -1;
        equippedTicks = 0;
    }

    private static boolean usable(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof SwordItem
                && (!stack.isDamageableItem() || stack.getMaxDamage() - stack.getDamageValue() > 10);
    }

    private static boolean eligible(Minecraft mc, Mob mob) {
        return mob instanceof Enemy && mob.isAlive() && mob.isAttackable() && !mob.isInvulnerable()
                && !mc.player.isAlliedTo(mob)
                && (!(mob instanceof NeutralMob neutral) || neutral.isAngryAt(mc.player))
                && mc.player.canInteractWithEntity(mob, 0) && mc.player.hasLineOfSight(mob);
    }
}
