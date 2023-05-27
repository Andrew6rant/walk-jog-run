package com.minenash.walk_jog_run;

import com.minenash.walk_jog_run.config.ServerConfig;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

import static com.minenash.walk_jog_run.WalkJogRun.stamina;

public class CustomHungerManager extends HungerManager {
    private final PlayerEntity player;

    public CustomHungerManager(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public void add(int hunger, float saturationModifier) {
        int max_stamina_plus_hunger = (player.getHungerManager().getFoodLevel() + hunger) * ServerConfig.STAMINA_PER_FOOD_LEVEL;
        stamina.put(player, MathHelper.clamp(stamina.get(player) + (hunger * 10), 0, max_stamina_plus_hunger));
        super.add(hunger, saturationModifier);
    }

    @Override
    public boolean isNotFull() {
        int max_stamina = player.getHungerManager().getFoodLevel() * ServerConfig.STAMINA_PER_FOOD_LEVEL;
        if (stamina.get(player) != max_stamina) {
            return true;
        }
        return super.isNotFull();
    }
}
