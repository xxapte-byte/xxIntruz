package pl.intruz.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.intruz.module.Module;
import pl.intruz.module.ModuleManager;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true)
    private void intruz$incognito(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        var me = MinecraftClient.getInstance().player;
        Module m = ModuleManager.get("Incognito");
        if (me == null || m == null || !m.enabled || m.text("Name").isBlank()) return;
        if (entry.getProfile().getId().equals(me.getUuid())) cir.setReturnValue(Text.literal(m.text("Name")));
    }
}
