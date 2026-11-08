package dev.spod.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface CarriedItemSync {
    @Invoker("ensureHasSentCarriedItem")
    void spod$syncCarriedItem();
}
