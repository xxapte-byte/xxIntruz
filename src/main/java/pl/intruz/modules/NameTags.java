package pl.intruz.modules;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import pl.intruz.module.Category;
import pl.intruz.render.RenderUtil;

import java.util.*;

/**
 * Nametagi rysowane na HUD: pozycja 3D gracza jest rzutowana na ekran, dzieki temu
 * nie zalezy od stanu renderera swiata i widac je przez sciany.
 * Wyglad: [ nick  HP  ping ] w ramce (kolor ramki = zdrowie), nad nia enchanty zbroi.
 */
public class NameTags extends pl.intruz.module.Module {
    private record Tag(float x, float y, String name, String hp, int hpColor, String ping, int border,
                       List<String> ench, String set, String weapon) {}

    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] ORDER = {"protection", "fire_protection", "blast_protection",
            "projectile_protection", "unbreaking", "mending", "thorns", "feather_falling", "depth_strider",
            "respiration", "aqua_affinity", "soul_speed", "swift_sneak"};

    private final List<Tag> tags = new ArrayList<>();

    public NameTags() {
        super("NameTags", Category.RENDER);
        addBool("Health", true);
        addBool("Ping", true);
        addBool("Enchants", true);
        addBool("Set", false);
        addBool("Weapon", false);
        addNum("Scale", 1.0, 0.5, 3, 0.1);
        addNum("Range", 128, 16, 256, 8);
    }

    @Override
    public void onWorldRender(WorldRenderContext c) {
        tags.clear();
        if (mc.world == null || mc.player == null) return;
        Vec3d cam = c.camera().getPos();
        float td = c.tickCounter().getTickDelta(false);
        Matrix4f view = RenderUtil.view(c);
        Matrix4f proj = c.projectionMatrix();
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();

        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.getCameraEntity() || p.isSpectator()) continue;
            double dist = p.distanceTo(mc.getCameraEntity());
            if (dist > num("Range")) continue;

            Vec3d pos = p.getLerpedPos(td);
            Vector4f v = new Vector4f((float) (pos.x - cam.x), (float) (pos.y + p.getHeight() + 0.5 - cam.y),
                    (float) (pos.z - cam.z), 1f);
            v.mul(view).mul(proj);
            if (v.w <= 0.05f) continue; // za kamera
            float sx = (v.x / v.w * 0.5f + 0.5f) * sw;
            float sy = (0.5f - v.y / v.w * 0.5f) * sh;
            if (sx < -200 || sx > sw + 200 || sy < -100 || sy > sh + 100) continue;

            float hpv = p.getHealth() + p.getAbsorptionAmount();
            float ratio = MathHelper.clamp(p.getHealth() / p.getMaxHealth(), 0f, 1f);
            int hpColor = 0xFF000000 | ((int) (255 * (1 - ratio)) << 16) | ((int) (255 * ratio) << 8) | 0x20;

            String ping = "";
            if (bool("Ping") && mc.getNetworkHandler() != null) {
                PlayerListEntry le = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
                if (le != null) ping = le.getLatency() + "ms";
            }

            List<String> ench = bool("Enchants") ? armorEnchantLines(p) : List.of();
            String set = bool("Set") ? setName(p) : "";
            String weapon = bool("Weapon") ? weaponLine(p) : "";

            tags.add(new Tag(sx, sy, p.getGameProfile().getName(),
                    bool("Health") ? String.format(Locale.ROOT, "%.0f", hpv) : "",
                    hpColor, ping, hpColor, ench, set, weapon));
        }
    }

    @Override
    public void onHud(DrawContext ctx) {
        if (mc.currentScreen != null) return;
        var tr = mc.textRenderer;
        float s = (float) num("Scale");

        for (Tag t : tags) {
            var ms = ctx.getMatrices();
            ms.push();
            ms.translate(t.x(), t.y(), 0);
            ms.scale(s, s, 1f);

            int nw = tr.getWidth(t.name());
            int hw = t.hp().isEmpty() ? 0 : 4 + tr.getWidth(t.hp());
            int pw = t.ping().isEmpty() ? 0 : 4 + tr.getWidth(t.ping());
            int w = nw + hw + pw;
            int x0 = -w / 2;

            // tlo + ramka
            ctx.fill(x0 - 3, -12, x0 + w + 3, 0, 0xA0000000);
            ctx.fill(x0 - 3, -12, x0 + w + 3, -11, t.border());
            ctx.fill(x0 - 3, -1, x0 + w + 3, 0, t.border());
            ctx.fill(x0 - 3, -12, x0 - 2, 0, t.border());
            ctx.fill(x0 + w + 2, -12, x0 + w + 3, 0, t.border());

            int tx = x0;
            ctx.drawTextWithShadow(tr, t.name(), tx, -10, 0xFFFFFFFF);
            tx += nw;
            if (!t.hp().isEmpty()) {
                tx += 4;
                ctx.drawTextWithShadow(tr, t.hp(), tx, -10, t.hpColor());
                tx += tr.getWidth(t.hp());
            }
            if (!t.ping().isEmpty()) {
                tx += 4;
                ctx.drawTextWithShadow(tr, t.ping(), tx, -10, 0xFFFFFFFF);
            }

            // enchanty zbroi nad ramka (mniejsza czcionka)
            if (!t.ench().isEmpty()) {
                ms.push();
                ms.translate(0, -14, 0);
                ms.scale(0.75f, 0.75f, 1f);
                int n = t.ench().size();
                for (int i = 0; i < n; i++) {
                    ctx.drawCenteredTextWithShadow(tr, t.ench().get(i), 0, -(n - i) * 10, 0xFFFFFFFF);
                }
                ms.pop();
            }
            // set i bron pod ramka
            int by = 3;
            if (!t.set().isEmpty()) {
                ctx.drawCenteredTextWithShadow(tr, t.set(), 0, by, 0xFF55FFFF);
                by += 10;
            }
            if (!t.weapon().isEmpty()) {
                ctx.drawCenteredTextWithShadow(tr, t.weapon(), 0, by, 0xFFFFFF55);
            }
            ms.pop();
        }
    }

    @Override
    public void onDisable() { tags.clear(); }

    // ---------------- dane o ekwipunku ----------------

    /** jedna linia na enchant: np. "Pro 4 Pro 4 Pro 4 Pro 4", "Unb 3 Unb 3 ..." */
    private List<String> armorEnchantLines(AbstractClientPlayerEntity p) {
        List<Map<String, Integer>> pieces = new ArrayList<>();
        LinkedHashSet<String> keys = new LinkedHashSet<>(Arrays.asList(ORDER));
        for (EquipmentSlot slot : ARMOR) {
            Map<String, Integer> m = new HashMap<>();
            collect(p.getEquippedStack(slot), m);
            pieces.add(m);
            keys.addAll(m.keySet());
        }
        List<String> out = new ArrayList<>();
        for (String k : keys) {
            StringBuilder sb = new StringBuilder();
            for (Map<String, Integer> m : pieces) {
                Integer lvl = m.get(k);
                if (lvl == null) continue;
                if (sb.length() > 0) sb.append(' ');
                sb.append(shortName(k)).append(' ').append(lvl);
            }
            if (sb.length() > 0) out.add(sb.toString());
            if (out.size() >= 6) break;
        }
        return out;
    }

    private String weaponLine(AbstractClientPlayerEntity p) {
        ItemStack w = p.getMainHandStack();
        if (w.isEmpty()) return "";
        Map<String, Integer> ench = new LinkedHashMap<>();
        collect(w, ench);
        StringBuilder sb = new StringBuilder(w.getItem().getName().getString());
        if (!ench.isEmpty()) {
            StringJoiner j = new StringJoiner(", ", " [", "]");
            for (var e : ench.entrySet()) j.add(shortName(e.getKey()) + " " + e.getValue());
            sb.append(j);
        }
        return sb.toString();
    }

    private String setName(AbstractClientPlayerEntity p) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (EquipmentSlot slot : ARMOR) {
            ItemStack s = p.getEquippedStack(slot);
            if (s.isEmpty()) continue;
            String path = Registries.ITEM.getId(s.getItem()).getPath();
            String mat = path.contains("_") ? path.substring(0, path.indexOf('_')) : path;
            counts.merge(mat, 1, Integer::sum);
        }
        if (counts.isEmpty()) return "Brak armora";
        StringBuilder sb = new StringBuilder();
        for (var e : counts.entrySet()) {
            if (sb.length() > 0) sb.append(" + ");
            sb.append(cap(e.getKey())).append(' ').append(e.getValue()).append("/4");
        }
        return sb.toString();
    }

    private static void collect(ItemStack s, Map<String, Integer> out) {
        if (s.isEmpty()) return;
        ItemEnchantmentsComponent e = EnchantmentHelper.getEnchantments(s);
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> en : e.getEnchantmentEntries()) {
            String key = en.getKey().getKey().map(k -> k.getValue().getPath()).orElse("?");
            out.merge(key, en.getIntValue(), Math::max);
        }
    }

    private static String shortName(String k) {
        return switch (k) {
            case "protection" -> "Pro";
            case "fire_protection" -> "FPro";
            case "blast_protection" -> "BPro";
            case "projectile_protection" -> "PPro";
            case "unbreaking" -> "Unb";
            case "mending" -> "Mend";
            case "thorns" -> "Thorns";
            case "feather_falling" -> "FF";
            case "depth_strider" -> "DS";
            case "respiration" -> "Resp";
            case "aqua_affinity" -> "Aqua";
            case "soul_speed" -> "SoulSpd";
            case "swift_sneak" -> "Swift";
            case "sharpness" -> "Sharp";
            case "fire_aspect" -> "FireAsp";
            case "knockback" -> "KB";
            case "looting" -> "Loot";
            case "sweeping_edge" -> "Sweep";
            case "bane_of_arthropods" -> "Bane";
            case "binding_curse" -> "Bind";
            case "vanishing_curse" -> "Van";
            default -> cap(k.replace('_', ' '));
        };
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
