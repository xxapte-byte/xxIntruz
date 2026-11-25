package pl.intruz;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import pl.intruz.gui.ClickGui;
import pl.intruz.module.Module;
import pl.intruz.module.ModuleManager;

public class IntruzClient implements ClientModInitializer {
    public static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        openKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("Open Intruz GUI", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, "Intruz"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new ClickGui());
            }
            if (mc.player == null) return;

            // bindy: klawisz wl/wyl modul (tylko gdy nie ma otwartego ekranu / czatu)
            long win = mc.getWindow().getHandle();
            for (Module m : ModuleManager.MODULES) {
                if (m.key > 0) {
                    boolean down = InputUtil.isKeyPressed(win, m.key);
                    if (down && !m.keyDown && mc.currentScreen == null) {
                        m.toggle();
                        ModuleManager.save();
                    }
                    m.keyDown = down;
                }
            }

            for (Module m : ModuleManager.MODULES) {
                if (m.enabled) m.onTick(mc);
            }
        });

        WorldRenderEvents.LAST.register(ctx -> {
            for (Module m : ModuleManager.MODULES) {
                if (m.enabled) m.onWorldRender(ctx);
            }
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            for (Module m : ModuleManager.MODULES) {
                if (m.enabled) m.onHud(ctx);
            }

            Module hud = ModuleManager.get("Hud");
            if (!hud.enabled) return;
            var tr = mc.textRenderer;
            if (hud.bool("Watermark")) {
                ctx.drawTextWithShadow(tr, "Intruz", 4, 4, 0xFFA03CE6);
            }
            if (hud.bool("ArrayList")) {
                int y = 4;
                var list = ModuleManager.MODULES.stream()
                        .filter(m -> m.enabled && !m.name.equals("Hud"))
                        .sorted((a, b) -> tr.getWidth(b.name) - tr.getWidth(a.name))
                        .toList();
                for (Module m : list) {
                    int x = ctx.getScaledWindowWidth() - tr.getWidth(m.name) - 4;
                    ctx.drawTextWithShadow(tr, m.name, x, y, 0xFFA03CE6);
                    y += 10;
                }
            }
        });
    }
}
