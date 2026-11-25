package pl.intruz.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import pl.intruz.module.Category;

public class AutoRespawn extends pl.intruz.module.Module {
    public AutoRespawn() { super("AutoRespawn", Category.PLAYER); }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.currentScreen instanceof DeathScreen) mc.player.requestRespawn();
    }
}
