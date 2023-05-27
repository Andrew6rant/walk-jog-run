package com.minenash.walk_jog_run.mixin;

import com.minenash.walk_jog_run.WalkJogRunClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    /*

    private static final int OFFSET_LEFT = -18;
    private static final int OFFSET_RIGHT = 16;

    @ModifyArg(method = "renderHotbar", index = 1, at = @At(value = "INVOKE", ordinal = 4, target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V"))
    public int walkJogRun$changeHotbarAttackIndicatorX(int old) {
        return old + (MinecraftClient.getInstance().player.getMainArm() == Arm.RIGHT ? OFFSET_RIGHT : OFFSET_LEFT);
    }

    @ModifyArg(method = "renderHotbar", index = 1, at = @At(value = "INVOKE", ordinal = 5, target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V"))
    public int walkJogRun$changeHotbarAttackIndicatorX2(int old) {
        return old + (MinecraftClient.getInstance().player.getMainArm() == Arm.RIGHT ? OFFSET_RIGHT : OFFSET_LEFT);
    }*/

}
