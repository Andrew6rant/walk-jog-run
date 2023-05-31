package com.minenash.walk_jog_run;

import com.minenash.walk_jog_run.config.ClientConfig;
import com.minenash.walk_jog_run.config.ServerConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class WalkJogRunClient implements ClientModInitializer {

    public static final KeyBinding STROLLING_KEYBIND = keybind();

    private static KeyBinding keybind() {
        KeyBinding binding = new KeyBinding("walkjogrun.keybind." + "strolling", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, KeyBinding.MOVEMENT_CATEGORY);
        KeyBindingHelper.registerKeyBinding(binding);
        return binding;
    }
    public static boolean isStrolling = false;
    boolean wasSprinting = false;
    boolean wasStrolling = false;

    public static int stamina = 200;
    private static final Identifier XP_STAMINA_TEXTURE = WalkJogRun.id("textures/gui/stamina_bar.png");

    private static final MinecraftClient client = MinecraftClient.getInstance();

    @Override
    public void onInitializeClient() {

        ClientConfig.init("walk-jog-run-client", ClientConfig.class);


        ClientTickEvents.END_WORLD_TICK.register(client -> {

            if (isSprinting() != wasSprinting) {
                if (wasSprinting)
                    setStrolling(wasStrolling);
                else {
                    wasStrolling = isStrolling;
                    setStrolling(false);
                }
                wasSprinting = isSprinting();
                return;
            }
            wasSprinting = isSprinting();

            while (!isSprinting() && STROLLING_KEYBIND.wasPressed())
                setStrolling(isStrolling = !isStrolling);
        });

        ClientPlayNetworking.registerGlobalReceiver(WalkJogRun.id("stamina"), (client1, handler, buf, responseSender) -> {
            stamina = buf.readInt();
        });

        ClientPlayNetworking.registerGlobalReceiver(WalkJogRun.id("sync_config"), (client1, handler, buf, responseSender) -> {
            ServerConfig.applyConfig(buf.readString());
        });

        ClientPlayConnectionEvents.DISCONNECT.register(WalkJogRun.id("sync_correct"), (handler, client1) -> {
            ServerConfig.read();

        });

        HudRenderCallback.EVENT.register( WalkJogRun.id("icon_render"), (matrix, tickDelta) -> {
            matrix.push();
            int max_stamina = client.player.getHungerManager().getFoodLevel() * ServerConfig.STAMINA_PER_FOOD_LEVEL;

            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.enableDepthTest();
            // stamina < max_stamina &&
            if (!client.player.isCreative()) {
                renderXPBarStamina(matrix, max_stamina);
            }

            matrix.pop();
        });

    }

    private void renderXPBarStamina(MatrixStack matrix, int max_stamina) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, XP_STAMINA_TEXTURE);
        int x = client.getWindow().getScaledWidth() / 2 - 91;
        int l = client.getWindow().getScaledHeight() - 32 + 5;

        int remainder = 20 - client.player.getHungerManager().getFoodLevel();
        int width2 = (int) (1F * 182 / 20 * remainder);
        int width = (int) (1F * (182 - width2) * stamina / max_stamina);

        int v_strolling_offset = isStrolling ? 9 : 3;

        // background
        DrawableHelper.drawTexture(matrix, x, l, 0, 0, 182, 3);
        // stamina bar fill
        DrawableHelper.drawTexture(matrix, x, l, 0, v_strolling_offset, width, 3);
        // disabled area fill (when player is hungry)
        DrawableHelper.drawTexture(matrix, (182 - width2) + x, l, 182 - width2, 6, width2, 3);
    }

    private boolean isSprinting() {
        return client.player.isSprinting();
    }

    private void setStrolling(boolean strolling) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(strolling);
        ClientPlayNetworking.send( WalkJogRun.id("strolling"), buf);
    }
}
