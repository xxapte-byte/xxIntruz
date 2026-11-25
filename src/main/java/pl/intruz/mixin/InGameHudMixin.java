package pl.intruz.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.intruz.module.Module;
import pl.intruz.module.ModuleManager;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    // dyniowy / powder snow overlay
    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void intruz$pumpkin(CallbackInfo ci) {
        Module m = ModuleManager.get("NoOverlay");
        if (m != null && m.enabled && m.bool("Pumpkin")) ci.cancel();
    }
}
