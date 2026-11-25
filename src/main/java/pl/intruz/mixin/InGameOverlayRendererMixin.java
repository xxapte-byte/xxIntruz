package pl.intruz.mixin;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.intruz.module.Module;
import pl.intruz.module.ModuleManager;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    private static boolean intruz$on(String setting) {
        Module m = ModuleManager.get("NoOverlay");
        return m != null && m.enabled && m.bool(setting);
    }

    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void intruz$fire(CallbackInfo ci) { if (intruz$on("Fire")) ci.cancel(); }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void intruz$water(CallbackInfo ci) { if (intruz$on("Water")) ci.cancel(); }

    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true)
    private static void intruz$wall(CallbackInfo ci) { if (intruz$on("InWall")) ci.cancel(); }
}
