package com.lagz.ghosthand.mixin;

import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(ClientPlayerEntity.class)
public class GhostHandMixin {
    private boolean wasUseKeyDown = false;
    private final Set<BlockPos> checkedPositions = new HashSet<>();

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.interactionManager == null) return;

        boolean useKeyDown = client.options.useKey.isPressed();
        boolean justPressed = useKeyDown && !wasUseKeyDown;
        wasUseKeyDown = useKeyDown;

        if (!justPressed) return;
        if (client.player.isSneaking()) return;

        if (client.crosshairTarget instanceof BlockHitResult hit) {
            BlockEntity be = client.world.getBlockEntity(hit.getBlockPos());
            if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof TrappedChestBlockEntity) {
                return;
            }
        }

        Vec3d eyePos = client.player.getEyePos();
        Vec3d lookVec = client.player.getRotationVec(1.0f);
        double range = client.player.getBlockInteractionRange();
        double step = 0.1;

        checkedPositions.clear();

        for (double d = step; d < range; d += step) {
            Vec3d point = eyePos.add(lookVec.multiply(d));
            BlockPos pos = BlockPos.ofFloored(point);

            if (checkedPositions.contains(pos)) continue;
            checkedPositions.add(pos);

            BlockEntity be = client.world.getBlockEntity(pos);
            if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof TrappedChestBlockEntity) {
                for (Hand hand : Hand.values()) {
                    BlockHitResult hitResult = new BlockHitResult(
                            Vec3d.ofCenter(pos), Direction.UP, pos, true
                    );
                    ActionResult result = client.interactionManager.interactBlock(client.player, hand, hitResult);
                    if (result instanceof ActionResult.Success || result instanceof ActionResult.Fail) {
                        client.player.swingHand(hand);
                        return;
                    }
                }
            }
        }
    }
}
