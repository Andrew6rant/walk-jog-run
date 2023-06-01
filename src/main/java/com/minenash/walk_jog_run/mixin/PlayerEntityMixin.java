package com.minenash.walk_jog_run.mixin;

import com.minenash.walk_jog_run.CustomHungerManager;
import com.minenash.walk_jog_run.WalkJogRun;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Shadow protected HungerManager hungerManager;

    @ModifyArg(method = "jump", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/entity/player/PlayerEntity;addExhaustion(F)V"))
    public float walkJogRun$removeSprintExhaustionFromJump(float exhaustion) {
        return 0.05F;
    }

    @ModifyArg(method = "increaseTravelMotionStats", at = @At(value = "INVOKE", ordinal = 3, target = "Lnet/minecraft/entity/player/PlayerEntity;addExhaustion(F)V"))
    public float walkJogRun$removeSprintExhaustionFromSprinting(float exhaustion) {
        return 0.0F;
    }

    @Redirect(method = "increaseTravelMotionStats", at = @At(value = "INVOKE", ordinal = 6, target = "Lnet/minecraft/entity/player/PlayerEntity;increaseStat(Lnet/minecraft/util/Identifier;I)V"))
    public void walkJogRun$increaseStrollingStat(PlayerEntity player, Identifier stat, int amount) {
        if (WalkJogRun.strolling.getOrDefault(player, false)) {
            player.increaseStat(WalkJogRun.STROLL_ONE_CM, amount);
        } else {
            player.increaseStat(stat, amount);
        }
    }

    // This mixin was taken from https://github.com/matthewperiut/NoHunger/blob/master/src/main/java/net/Slainlight/NoHunger/mixin/MixinPlayerEntity.java
    @Inject(at = @At("RETURN"), method = "<init>")
    private void onInit(CallbackInfo info) {
        hungerManager = new CustomHungerManager((PlayerEntity) (Object) this);
    }
}