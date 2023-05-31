package com.minenash.walk_jog_run.mixin;

import com.minenash.walk_jog_run.WalkJogRunClient;
import com.minenash.walk_jog_run.config.ServerConfig;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin extends Input {
    // This mixin was modified from https://github.com/Kaitaki/sprint-o-meter/blob/1.19.2/src/main/java/com/paperscp/sprintometer/mixins/KeyboardInputMixin.java
    @Mutable @Final @Shadow private final GameOptions settings;
    public KeyboardInputMixin(GameOptions settings) {
        this.settings = settings;
    }

    @Inject(at = @At("TAIL"), method = "tick")
    public void tick(boolean slowDown, float f, CallbackInfo ci) {
        this.jumping = WalkJogRunClient.stamina > ServerConfig.ALLOWED_JUMP_SWIM_STAMINA && this.settings.jumpKey.isPressed();
    }
}
