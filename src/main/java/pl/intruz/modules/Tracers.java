package pl.intruz.modules;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import pl.intruz.module.Category;
import pl.intruz.render.RenderUtil;

public class Tracers extends pl.intruz.module.Module {
    public Tracers() {
        super("Tracers", Category.RENDER);
        addBool("Players", true);
        addBool("Mobs", false);
        addNum("Range", 128, 16, 256, 8);
        addNum("Width", 2, 1, 5, 0.5);
        addMode("ColorMode", 0, "Custom", "Distance");
        addColor("Player", 255, 60, 60);
        addColor("Mob", 60, 255, 100);
    }

    @Override
    public void onWorldRender(WorldRenderContext c) {
        if (mc.world == null || mc.player == null) return;
        Entity camEnt = mc.getCameraEntity();
        if (camEnt == null) return;
        Vec3d cam = c.camera().getPos();
        float td = c.tickCounter().getTickDelta(false);
        Matrix4f m = RenderUtil.view(c);
        Vec3d look = Vec3d.fromPolar(c.camera().getPitch(), c.camera().getYaw()).multiply(0.3);
        double range = num("Range");
        boolean byDist = mode("ColorMode") == 1;

        BufferBuilder b = RenderUtil.begin(false, (float) num("Width"));
        for (Entity e : mc.world.getEntities()) {
            if (e == camEnt || !(e instanceof LivingEntity)) continue;
            boolean player = e instanceof PlayerEntity;
            if (player && (!bool("Players") || ((PlayerEntity) e).isSpectator())) continue;
            if (!player && !bool("Mobs")) continue;
            double dist = e.distanceTo(camEnt);
            if (dist > range) continue;

            int rgb;
            if (byDist && player) {
                float t = (float) Math.min(1.0, dist / range);
                rgb = ((int) (255 * (1 - t)) << 16) | ((int) (255 * t) << 8) | 60;
            } else {
                rgb = color(player ? "Player" : "Mob");
            }
            Vec3d p = e.getLerpedPos(td);
            float ex = (float) (p.x - cam.x), ey = (float) (p.y + e.getHeight() / 2 - cam.y), ez = (float) (p.z - cam.z);
            RenderUtil.line(b, m, (float) look.x, (float) look.y, (float) look.z, ex, ey, ez, 0xFF000000 | rgb);
        }
        RenderUtil.end(b);
    }
}
