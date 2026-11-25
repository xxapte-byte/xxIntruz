package pl.intruz.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.intruz.module.Module;
import pl.intruz.module.ModuleManager;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void intruz$noBlockPush(double x, double z, CallbackInfo ci) {
        Module m = ModuleManager.get("NoPush");
        if (m != null && m.enabled && m.bool("Blocks")) ci.cancel();
    }
}
