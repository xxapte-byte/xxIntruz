package pl.intruz.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import pl.intruz.module.Category;

public class Sprint extends pl.intruz.module.Module {
    public Sprint() { super("Sprint", Category.MOVEMENT); }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p.input.movementForward > 0 && !p.isSneaking() && !p.horizontalCollision
                && p.getHungerManager().getFoodLevel() > 6) {
            p.setSprinting(true);
        }
    }
}
