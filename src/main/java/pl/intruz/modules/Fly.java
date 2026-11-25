package pl.intruz.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import pl.intruz.module.Category;

public class Fly extends pl.intruz.module.Module {
    public Fly() {
        super("Fly", Category.MOVEMENT);
        addNum("Speed", 2, 1, 10, 0.5);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        p.getAbilities().allowFlying = true;
        p.getAbilities().setFlySpeed((float) (0.05 * num("Speed")));
    }

    @Override
    public void onDisable() {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        p.getAbilities().setFlySpeed(0.05f);
        if (!p.isCreative() && !p.isSpectator()) {
            p.getAbilities().allowFlying = false;
            p.getAbilities().flying = false;
        }
    }
}
