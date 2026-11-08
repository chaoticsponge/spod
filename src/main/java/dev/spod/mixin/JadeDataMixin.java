package dev.spod.mixin;

import dev.spod.HiveJade;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "snownee.jade.impl.ObjectDataCenter", remap = false)
public abstract class JadeDataMixin {
    @Inject(method = "setServerData", at = @At("HEAD"), cancellable = true)
    private static void spod$receiveHive(CompoundTag tag, CallbackInfo ci) {
        if (HiveJade.receive(tag)) {
            ci.cancel();
        }
    }
}
