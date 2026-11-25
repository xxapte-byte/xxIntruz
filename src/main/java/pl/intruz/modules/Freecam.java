package pl.intruz.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.OtherClientPlayerEntity;
import pl.intruz.module.Category;

public class Freecam extends pl.intruz.module.Module {
    private OtherClientPlayerEntity cam;
    private Input savedInput, emptyInput;
    private float savedYaw, savedPitch;

    public Freecam() {
        super("Freecam", Category.PLAYER);
        addNum("Speed", 1.0, 0.2, 5, 0.1);
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) { enabled = false; return; }
        cam = new OtherClientPlayerEntity(mc.world, mc.player.getGameProfile());
        cam.copyPositionAndRotation(mc.player);
        savedYaw = mc.player.getYaw();
        savedPitch = mc.player.getPitch();
        savedInput = mc.player.input;
        emptyInput = new Input();
        mc.player.input = emptyInput;      // postac stoi w miejscu
        mc.setCameraEntity(cam);
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            if (mc.player.input == emptyInput && savedInput != null) mc.player.input = savedInput;
            mc.player.setYaw(savedYaw);
            mc.player.setPitch(savedPitch);
            mc.setCameraEntity(mc.player);
        }
        cam = null;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (cam == null || cam.getWorld() != mc.world) { setEnabled(false); return; }
        mc.player.input = emptyInput;
        if (mc.player.input != emptyInput) return;

        cam.prevX = cam.getX(); cam.prevY = cam.getY(); cam.prevZ = cam.getZ();
        cam.prevYaw = cam.getYaw(); cam.prevPitch = cam.getPitch();

        double f = (mc.options.forwardKey.isPressed() ? 1 : 0) - (mc.options.backKey.isPressed() ? 1 : 0);
        double s = (mc.options.rightKey.isPressed() ? 1 : 0) - (mc.options.leftKey.isPressed() ? 1 : 0);
        double v = (mc.options.jumpKey.isPressed() ? 1 : 0) - (mc.options.sneakKey.isPressed() ? 1 : 0);
        double sp = num("Speed");
        double yaw = Math.toRadians(mc.player.getYaw());
        double dx = (-Math.sin(yaw) * f - Math.cos(yaw) * s) * sp;
        double dz = (Math.cos(yaw) * f - Math.sin(yaw) * s) * sp;

        cam.setPosition(cam.getX() + dx, cam.getY() + v * sp, cam.getZ() + dz);
        cam.setYaw(mc.player.getYaw());
        cam.setPitch(mc.player.getPitch());
    }
}
