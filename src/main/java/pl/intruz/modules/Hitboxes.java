package pl.intruz.modules;

import net.minecraft.client.MinecraftClient;
import pl.intruz.module.Category;

/** Pokazuje hitboxy encji (to samo co F3+B). Tylko wizualnie. */
public class Hitboxes extends pl.intruz.module.Module {
    public Hitboxes() { super("Hitboxes", Category.RENDER); }

    @Override
    public void onTick(MinecraftClient mc) {
        mc.getEntityRenderDispatcher().setRenderHitboxes(true);
    }

    @Override
    public void onDisable() {
        if (mc.getEntityRenderDispatcher() != null) mc.getEntityRenderDispatcher().setRenderHitboxes(false);
    }
}
