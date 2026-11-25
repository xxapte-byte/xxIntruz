package pl.intruz.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import pl.intruz.mixin.MinecraftClientAccessor;
import pl.intruz.module.Category;

public class FastPlace extends pl.intruz.module.Module {
    public FastPlace() {
        super("FastPlace", Category.WORLD);
        // Blocks = tylko gdy trzymasz blok; Items = tylko inne przedmioty (perly, sniezki...);
        // Everything = zawsze (tez drzwi, przyciski, dzwignie, skrzynie...)
        addMode("Mode", 2, "Blocks", "Items", "Everything");
        addNum("Delay", 0, 0, 4, 1);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        boolean block = mc.player.getMainHandStack().getItem() instanceof BlockItem
                || mc.player.getOffHandStack().getItem() instanceof BlockItem;
        int mode = mode("Mode");
        boolean apply = mode == 2 || (mode == 0 && block) || (mode == 1 && !block);
        if (!apply) return;
        MinecraftClientAccessor acc = (MinecraftClientAccessor) mc;
        int d = (int) num("Delay");
        if (acc.getItemUseCooldown() > d) acc.setItemUseCooldown(d);
    }
}
