package com.minenash.walk_jog_run;

import com.minenash.walk_jog_run.config.ServerConfig;
import com.minenash.walk_jog_run.mixin.LivingEntityAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.StatFormatter;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static net.minecraft.server.command.CommandManager.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class WalkJogRun implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("walk-jog-run");

	private static final UUID BASE_SPEED_MODIFIER_ID = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278C");
	private static EntityAttributeModifier BASE_SPEED_MODIFIER;

	private static final UUID STROLLING_SPEED_MODIFIER_ID = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278E");
	private static EntityAttributeModifier STROLLING_SPEED_MODIFIER;
	public static final Identifier STROLL_ONE_CM = id("stroll_one_cm");

	public static final Map<PlayerEntity, Boolean> strolling = new HashMap<>();


	public static final Map<PlayerEntity, Float> stamina = new HashMap<>();

	@Override
	public void onInitialize() {

		ServerConfig.read();
		updateModifiers();

		Registry.register(Registries.CUSTOM_STAT, "stroll_one_cm", STROLL_ONE_CM);
		Stats.CUSTOM.getOrCreateStat(STROLL_ONE_CM, StatFormatter.DISTANCE);



		ServerPlayNetworking.registerGlobalReceiver( id("strolling"), (server, player, handler, buf, responseSender) -> {
			EntityAttributeInstance movement = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
			boolean strollingP = buf.readBoolean();
			strolling.put(player, strollingP);

			if (strollingP) {
				movement.addTemporaryModifier(STROLLING_SPEED_MODIFIER);
			}
			else {
				movement.removeModifier(STROLLING_SPEED_MODIFIER);
			}
		});

		ServerTickEvents.START_SERVER_TICK.register(id("stamina"), server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

				EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
				if (instance != null) {
					if (!instance.hasModifier(BASE_SPEED_MODIFIER)) {
						instance.addTemporaryModifier(BASE_SPEED_MODIFIER);
					}
					else if (instance.getModifier(BASE_SPEED_MODIFIER_ID).getValue() != BASE_SPEED_MODIFIER.getValue()) {
						instance.removeModifier(BASE_SPEED_MODIFIER_ID);
						instance.addTemporaryModifier(BASE_SPEED_MODIFIER);
					}
				}

				float max_stamina = player.getHungerManager().getFoodLevel() * 1F * ServerConfig.STAMINA_PER_FOOD_LEVEL;
				float player_stamina = stamina.getOrDefault(player, max_stamina);

				if (!player.isCreative()) {
					//System.out.println(player_stamina);
					float speed = (player.horizontalSpeed - player.prevHorizontalSpeed) * 10F;
					float recovery_penalty = 0F;
					float swimming_penalty = 0F;
					int bar_segment = (ServerConfig.STAMINA_PER_FOOD_LEVEL * 20) / 18;
					//System.out.println(bar_segment);
					if (player_stamina < (bar_segment * 3)) {
						recovery_penalty = 1F;
						player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 210, 0, false, false));
						if (player_stamina < bar_segment * 2) {
							player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 210, 0, false, false));
							if (player_stamina < bar_segment) {
								player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 210, 0, false, false));
								if(player_stamina <= 0) {
									player.setSprinting(false);
									player.setSwimming(false);
									player.stopFallFlying();
									player.stopRiding();
									if(player.isUsingItem() && !player.getActiveItem().isFood()){
										player.stopUsingItem();
									}
									player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 210, 1, false, false));
								}
							}
						}
					}
					if (player.isTouchingWater()) { // I have to use an if else if chain because I do not want to apply multiple penalties at once
						swimming_penalty = 1.2F;
						if (player.isSubmergedInWater()) {
							swimming_penalty = 1.4F;
						}
					}
					if(player.isFallFlying()) { // using elytra
						player_stamina -= (speed/10);
						//LOGGER.info("speed: " + speed+ ", stamina: "+player_stamina);
					} else if (player.isSprinting()) {
						//System.out.println("sprinting");
						player_stamina -= Math.min((speed/1.2) + swimming_penalty, 5F);
						//LOGGER.info("speed: " + speed+ ", stamina: "+player_stamina+", isfallflying: "+player.isFallFlying());
					} else {
						float recovery_speed = strolling.getOrDefault(player, false) ? ServerConfig.STAMINA_RECOVERY_STROLLING : ServerConfig.STAMINA_RECOVERY_WALKING;
						//LOGGER.info("total:"+(recovery_speed - speed - recovery_penalty - (swimming_penalty * 2))+", recovery_speed: " + recovery_speed+ ", speed: "+speed+", recovery_penalty: "+recovery_penalty+", swimming_penalty: "+swimming_penalty);
						player_stamina += Math.min(recovery_speed - speed - recovery_penalty - (swimming_penalty * 2), 4F);

					}
				}
				setStamina(player, MathHelper.clamp(player_stamina, 0F, max_stamina));
			}
		});

		ServerPlayConnectionEvents.JOIN.register(id("sync_config"), (handler, sender, server) -> {
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeString(ServerConfig.JSON);
			sender.sendPacket(id("sync_config"), buf);
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("walkjogrun").then(literal("reload")
				.executes(context -> {
					ServerConfig.read();
					updateModifiers();
					for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
						PacketByteBuf buf = PacketByteBufs.create();
						buf.writeString(ServerConfig.JSON);
						ServerPlayNetworking.send(player, id("sync_config"), buf);
					}

					context.getSource().sendMessage(Text.literal("Walk Jog Run: Config reloaded"));
					return 1;
				})
			));
		});

	}

	public static void updateModifiers() {
		LivingEntityAccessor.setSPRINTING_SPEED_BOOST(new EntityAttributeModifier(LivingEntityAccessor.getSPRINTING_SPEED_BOOST_ID(), "Sprinting speed boost", ServerConfig.SPRINTING_SPEED_MODIFIER, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
		STROLLING_SPEED_MODIFIER = new EntityAttributeModifier(STROLLING_SPEED_MODIFIER_ID, "WalkJogRun: Strolling speed modification",
				ServerConfig.STROLLING_SPEED_MODIFIER, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
		BASE_SPEED_MODIFIER = new EntityAttributeModifier(BASE_SPEED_MODIFIER_ID, "WalkJogRun: Base speed modification",
				ServerConfig.BASE_WALKING_SPEED_MODIFIER, EntityAttributeModifier.Operation.MULTIPLY_BASE);
	}

	private static void setStamina(ServerPlayerEntity player, float staminaP) {
		stamina.put(player, staminaP);
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeFloat(staminaP);
		ServerPlayNetworking.send(player, WalkJogRun.id("stamina"), buf);
	}

	public static Identifier id(String str) {
		return new Identifier("walk-jog-run", str);
	}

}
