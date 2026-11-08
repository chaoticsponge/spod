package dev.spod.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.spod.PlacementOrientation;
import dev.spod.PlacementRotation;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MultiPlayerGameMode.class)
public abstract class PlacementMixin {
    @WrapMethod(method = "useItemOn")
    private InteractionResult spod$orient(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
                                          Operation<InteractionResult> original) {
        var facing = PlacementOrientation.facing();
        var item = player.getItemInHand(hand).getItem();
        if (facing == null || !(item instanceof BlockItem blockItem)
                || !PlacementOrientation.supports(blockItem.getBlock())) {
            return original.call(player, hand, hit);
        }
        PlacementRotation rotation = PlacementRotation.forFacing(facing, blockItem.getBlock() == Blocks.OBSERVER);
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        try {
            player.setYRot(rotation.yaw());
            player.setXRot(rotation.pitch());
            player.connection.send(new ServerboundMovePlayerPacket.Rot(rotation.yaw(), rotation.pitch(), player.onGround()));
            return original.call(player, hand, hit);
        } finally {
            player.setYRot(yaw);
            player.setXRot(pitch);
            player.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround()));
        }
    }
}
