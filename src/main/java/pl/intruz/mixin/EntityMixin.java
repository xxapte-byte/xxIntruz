package pl.intruz.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.intruz.module.Module;
import pl.intruz.module.ModuleManager;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void intruz$noEntityPush(Entity other, CallbackInfo ci) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        Module m = ModuleManager.get("NoPush");
        if (m != null && m.enabled && m.bool("Entities")) ci.cancel();
    }
}
