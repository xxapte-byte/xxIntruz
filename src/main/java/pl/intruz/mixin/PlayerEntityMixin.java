package pl.intruz.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.intruz.module.Module;
import pl.intruz.module.ModuleManager;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "isPushedByFluids", at = @At("HEAD"), cancellable = true)
    private void intruz$noFluidPush(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        Module m = ModuleManager.get("NoPush");
        if (m != null && m.enabled && m.bool("Fluids")) cir.setReturnValue(false);
    }

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void intruz$incognitoName(CallbackInfoReturnable<Text> cir) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        Module m = ModuleManager.get("Incognito");
        if (m != null && m.enabled && !m.text("Name").isBlank()) cir.setReturnValue(Text.literal(m.text("Name")));
    }
}
