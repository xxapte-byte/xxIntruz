package pl.intruz.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import pl.intruz.module.Category;

public class NoBadEffects extends pl.intruz.module.Module {
    public NoBadEffects() {
        super("NoBadEffects", Category.RENDER);
        addBool("Nausea", true);
        addBool("Blindness", true);
        addBool("Darkness", true);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (bool("Nausea")) mc.player.removeStatusEffect(StatusEffects.NAUSEA);
        if (bool("Blindness")) mc.player.removeStatusEffect(StatusEffects.BLINDNESS);
        if (bool("Darkness")) mc.player.removeStatusEffect(StatusEffects.DARKNESS);
    }
}
