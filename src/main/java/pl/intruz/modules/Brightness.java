package pl.intruz.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import pl.intruz.module.Category;

public class Brightness extends pl.intruz.module.Module {
    public Brightness() { super("Brightness", Category.RENDER); }

    @Override
    public void onTick(MinecraftClient mc) {
        mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
    }

    @Override
    public void onDisable() {
        if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
}
