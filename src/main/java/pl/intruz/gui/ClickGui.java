package pl.intruz.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import pl.intruz.module.Category;
import pl.intruz.module.Module;
import pl.intruz.module.ModuleManager;
import pl.intruz.module.Setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Jedno okno: pasek boczny (szukajka + kategorie + Panic mode) i siatka kart modulow. */
public class ClickGui extends Screen {
    private static final int INTRO_MS = 1400;
    private static final int SW = 100; // szerokosc paska bocznego
    private static final int PURPLE = 0xFFA03CE6;
    private static final Setting BIND = new Setting("Bind", true);

    private static final Map<String, String> DESC = Map.ofEntries(
            Map.entry("Sprint", "Automatically sprints while moving forward"),
            Map.entry("Fly", "Lets you fly with adjustable speed"),
            Map.entry("AutoRespawn", "Respawns you instantly after death"),
            Map.entry("Brightness", "Removes darkness with night vision"),
            Map.entry("NoBadEffects", "Removes nausea, blindness and darkness"),
            Map.entry("Hud", "Watermark and list of enabled modules"),
            Map.entry("Tracers", "Draws lines to players through walls"),
            Map.entry("Hitboxes", "Shows entity hitboxes"),
            Map.entry("NameTags", "Nametags with health, ping and gear"),
            Map.entry("NoOverlay", "Hides fire, water, wall and pumpkin overlays"),
            Map.entry("Freecam", "Detached camera, your body stays in place"),
            Map.entry("FastPlace", "Removes the delay between placing and using"),
            Map.entry("NoPush", "Stops pushes from entities, blocks and fluids"),
            Map.entry("Incognito", "Changes your displayed name on this client"));

    private interface Click { void on(double mx, double my, int button); }
    private record Hit(int x, int y, int w, int h, boolean clip, Click c) {}

    private final List<Hit> hits = new ArrayList<>();
    private final long opened = System.currentTimeMillis();
    private Category cat = Category.WORLD;
    private Module openModule;
    private double scroll, maxScroll;
    private int clipY0, clipY1;
    private Setting dragging, editing;
    private int dragX, dragW;
    private Module listening;
    private boolean searching;
    private String search = "";

    public ClickGui() { super(Text.literal("Intruz")); }

    @Override
    public boolean shouldPause() { return false; }

    private boolean introDone() { return System.currentTimeMillis() - opened >= INTRO_MS; }

    // ---------------- pomocnicze ----------------

    private static String keyName(int k) {
        if (k <= 0) return "NONE";
        try {
            return InputUtil.fromKeyCode(k, 0).getLocalizedText().getString().toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return "KEY" + k;
        }
    }

    private static void rr(DrawContext c, int x1, int y1, int x2, int y2, int r, int col) {
        c.fill(x1 + r, y1, x2 - r, y2, col);
        c.fill(x1, y1 + r, x2, y2 - r, col);
        if (r > 1) c.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, col);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void hit(int x, int y, int w, int h, boolean clip, Click c) { hits.add(new Hit(x, y, w, h, clip, c)); }

    private void toggleSwitch(DrawContext ctx, int x, int y, boolean on) {
        rr(ctx, x, y, x + 22, y + 10, 2, on ? PURPLE : 0xFF2A2A2A);
        int kx = on ? x + 13 : x + 2;
        ctx.fill(kx, y + 2, kx + 7, y + 8, 0xFFFFFFFF);
    }

    private void gearIcon(DrawContext ctx, int x, int y, boolean hover) {
        rr(ctx, x, y, x + 14, y + 14, 1, hover ? 0xFF2A2A2A : 0xFF1F1F1F);
        int[] kn = {8, 4, 7};
        for (int k = 0; k < 3; k++) {
            ctx.fill(x + 3, y + 4 + k * 3, x + 11, y + 5 + k * 3, 0xFF8A8A8A);
            ctx.fill(x + kn[k], y + 3 + k * 3, x + kn[k] + 2, y + 6 + k * 3, 0xFFFFFFFF);
        }
    }

    private void smallCentered(DrawContext ctx, String text, float cx, float y, int color, float scale) {
        var ms = ctx.getMatrices();
        ms.push();
        ms.translate(cx, y, 0);
        ms.scale(scale, scale, 1f);
        ctx.drawCenteredTextWithShadow(textRenderer, text, 0, 0, color);
        ms.pop();
    }

    private void applyDrag(double mx) {
        if (dragging == null) return;
        double f = Math.max(0, Math.min(1, (mx - dragX) / dragW));
        double v = dragging.min + f * (dragging.max - dragging.min);
        v = Math.round(v / dragging.step) * dragging.step;
        dragging.value = Math.max(dragging.min, Math.min(dragging.max, v));
    }

    private List<Module> visible() {
        if (search.isEmpty()) return ModuleManager.in(cat);
        List<Module> out = new ArrayList<>();
        String q = search.toLowerCase(Locale.ROOT);
        for (Module m : ModuleManager.MODULES) if (m.name.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        return out;
    }

    // ---------------- render ----------------

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x90000000);
        hits.clear();
        long t = System.currentTimeMillis() - opened;

        if (t < INTRO_MS) {
            double a = t < 400 ? t / 400.0 : (t > 1000 ? (INTRO_MS - t) / 400.0 : 1.0);
            int alpha = (int) (255 * Math.max(0, Math.min(1, a)));
            if (alpha < 5) return;
            float scale = 5f + (float) (t / 1400.0);
            var ms = ctx.getMatrices();
            ms.push();
            ms.translate(width / 2f, height / 2f, 0);
            ms.scale(scale, scale, 1f);
            ctx.drawCenteredTextWithShadow(textRenderer, "Intruz", 0, -4, (alpha << 24) | 0xA03CE6);
            ms.pop();
            return;
        }

        int ww = Math.min(480, width - 16), wh = Math.min(280, height - 16);
        int wx = (width - ww) / 2, wy = (height - wh) / 2;
        rr(ctx, wx, wy, wx + ww, wy + wh, 2, 0xFF222222);
        rr(ctx, wx + 1, wy + 1, wx + ww - 1, wy + wh - 1, 2, 0xFF0C0C0C);

        // naglowek
        smallCentered(ctx, "Intruz", wx + SW / 2f + 4, wy + 12, PURPLE, 1.6f);
        ctx.fill(wx + 8, wy + 38, wx + ww - 8, wy + 39, 0xFF1C1C1C);

        int ax0 = wx + SW + 8, ax1 = wx + ww - 10, ay0 = wy + 46, ay1 = wy + wh - 10;
        String title = openModule != null ? openModule.name + " - settings"
                : (search.isEmpty() ? cat.label : "Search results");
        ctx.drawTextWithShadow(textRenderer, title, ax0, wy + 15, 0xFFFFFFFF);
        String hint = "RMB: settings   MMB: bind   P: close";
        ctx.drawTextWithShadow(textRenderer, hint, ax1 - textRenderer.getWidth(hint), wy + 15, 0xFF666666);

        drawSidebar(ctx, mx, my, wx, wy, wh);

        if (openModule == null) drawModules(ctx, mx, my, ax0, ay0, ax1, ay1);
        else drawSettings(ctx, mx, my, ax0, ay0, ax1, ay1);
    }

    private void drawSidebar(DrawContext ctx, int mx, int my, int wx, int wy, int wh) {
        int sx0 = wx + 8, sx1 = wx + SW - 2;

        // szukajka
        rr(ctx, sx0, wy + 46, sx1, wy + 62, 2, searching ? 0xFF2A2A2A : 0xFF171717);
        String st = search.isEmpty() && !searching ? "Search" : search + (searching ? "_" : "");
        ctx.drawTextWithShadow(textRenderer, st, sx0 + 6, wy + 50, search.isEmpty() && !searching ? 0xFF777777 : 0xFFFFFFFF);
        hit(sx0, wy + 46, sx1 - sx0, 16, false, (x, y, b) -> searching = true);

        // kategorie
        int y = wy + 74;
        for (Category c : Category.values()) {
            if (ModuleManager.in(c).isEmpty()) continue;
            boolean sel = search.isEmpty() && c == cat;
            boolean hover = in(mx, my, sx0, y, sx1 - sx0, 20);
            if (sel) {
                rr(ctx, sx0, y, sx1, y + 20, 2, 0xFF161016);
                ctx.fill(sx0, y + 4, sx0 + 2, y + 16, PURPLE);
            }
            ctx.drawTextWithShadow(textRenderer, c.label, sx0 + 10, y + 6,
                    sel ? PURPLE : (hover ? 0xFFFFFFFF : 0xFFCCCCCC));
            final Category cc = c;
            hit(sx0, y, sx1 - sx0, 20, false, (x, yy, b) -> {
                cat = cc; openModule = null; scroll = 0; search = "";
            });
            y += 24;
        }

        // Panic mode
        int py = wy + wh - 28;
        boolean ph = in(mx, my, sx0, py, sx1 - sx0, 20);
        rr(ctx, sx0, py, sx1, py + 20, 2, ph ? 0xFF1E1E1E : 0xFF141414);
        ctx.drawCenteredTextWithShadow(textRenderer, "Panic mode", (sx0 + sx1) / 2, py + 6, 0xFF8A8A8A);
        hit(sx0, py, sx1 - sx0, 20, false, (x, yy, b) -> {
            for (Module m : ModuleManager.MODULES) m.setEnabled(false);
            ModuleManager.save();
            close();
        });
    }

    private void drawModules(DrawContext ctx, int mx, int my, int ax0, int ay0, int ax1, int ay1) {
        List<Module> vis = visible();
        int gap = 6, cw = (ax1 - ax0 - gap) / 2, ch = 36;
        int rows = (vis.size() + 1) / 2;
        maxScroll = Math.max(0, rows * (ch + gap) - (ay1 - ay0));
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        clipY0 = ay0; clipY1 = ay1;

        ctx.enableScissor(ax0, ay0, ax1, ay1);
        for (int i = 0; i < vis.size(); i++) {
            Module m = vis.get(i);
            int cx = ax0 + (i % 2) * (cw + gap);
            int cy = ay0 + (i / 2) * (ch + gap) - (int) scroll;
            if (cy + ch < ay0 || cy > ay1) continue;

            boolean hover = in(mx, my, cx, cy, cw, ch) && my >= ay0 && my < ay1;
            rr(ctx, cx, cy, cx + cw, cy + ch, 2, hover ? 0xFF2A2A2A : 0xFF1E1E1E);
            rr(ctx, cx + 1, cy + 1, cx + cw - 1, cy + ch - 1, 2, hover ? 0xFF161616 : 0xFF111111);

            int tx = cx + cw - 8 - 22;
            int bx = tx - 4 - 26;
            int gx = bx - 4 - 14;
            int midY = cy + ch / 2;

            ctx.drawTextWithShadow(textRenderer, m.name, cx + 8, cy + 6, 0xFFFFFFFF);
            var ms = ctx.getMatrices();
            ms.push();
            ms.translate(cx + 8, cy + 18, 0);
            ms.scale(0.75f, 0.75f, 1f);
            List<OrderedText> lines = textRenderer.wrapLines(Text.literal(DESC.getOrDefault(m.name, "")),
                    (int) ((gx - cx - 14) / 0.75f));
            for (int k = 0; k < Math.min(2, lines.size()); k++)
                ctx.drawTextWithShadow(textRenderer, lines.get(k), 0, k * 10, 0xFF7C7C7C);
            ms.pop();

            // przyciski: ustawienia, bind, przelacznik
            gearIcon(ctx, gx, midY - 7, in(mx, my, gx, midY - 7, 14, 14));
            boolean bh = in(mx, my, bx, midY - 7, 26, 14);
            rr(ctx, bx, midY - 7, bx + 26, midY + 7, 1, bh ? 0xFF2A2A2A : 0xFF1F1F1F);
            String bl = listening == m ? "..." : (m.key > 0 ? keyName(m.key) : "-");
            if (bl.length() > 5) bl = bl.substring(0, 5);
            smallCentered(ctx, bl, bx + 13, midY - 3, listening == m ? 0xFFFFFF55 : 0xFFBBBBBB, 0.75f);
            toggleSwitch(ctx, tx, midY - 5, m.enabled);

            hit(cx, cy, cw, ch, true, (x, y, b) -> {
                if (b == 0) m.toggle();
                else if (b == 1) { openModule = m; scroll = 0; }
                else if (b == 2) listening = m;
            });
            hit(gx, midY - 7, 14, 14, true, (x, y, b) -> { openModule = m; scroll = 0; });
            hit(bx, midY - 7, 26, 14, true, (x, y, b) -> listening = m);
            hit(tx, midY - 5, 22, 10, true, (x, y, b) -> m.toggle());
        }
        ctx.disableScissor();
    }

    private void drawSettings(DrawContext ctx, int mx, int my, int ax0, int ay0, int ax1, int ay1) {
        Module m = openModule;
        int w = ax1 - ax0;

        // przycisk Back + przelacznik modulu
        boolean bh = in(mx, my, ax0, ay0, 50, 16);
        rr(ctx, ax0, ay0, ax0 + 50, ay0 + 16, 2, bh ? 0xFF2A2A2A : 0xFF181818);
        ctx.drawCenteredTextWithShadow(textRenderer, "< Back", ax0 + 25, ay0 + 4, 0xFFFFFFFF);
        hit(ax0, ay0, 50, 16, false, (x, y, b) -> { openModule = null; scroll = 0; });
        ctx.drawTextWithShadow(textRenderer, m.enabled ? "Enabled" : "Disabled",
                ax1 - 22 - 8 - 44, ay0 + 4, m.enabled ? PURPLE : 0xFF888888);
        toggleSwitch(ctx, ax1 - 22, ay0 + 3, m.enabled);
        hit(ax1 - 22, ay0 + 3, 22, 10, false, (x, y, b) -> m.toggle());

        int top = ay0 + 22;
        clipY0 = top; clipY1 = ay1;
        List<Setting> list = new ArrayList<>();
        list.add(BIND);
        list.addAll(m.settings);

        int total = 0;
        for (Setting s : list) total += rowH(s) + 4;
        maxScroll = Math.max(0, total - (ay1 - top));
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        ctx.enableScissor(ax0, top, ax1, ay1);
        int y = top - (int) scroll;
        for (Setting s : list) {
            int h = rowH(s);
            if (y + h >= top && y <= ay1) drawRow(ctx, mx, my, m, s, ax0, y, w, h);
            y += h + 4;
        }
        ctx.disableScissor();
    }

    private static int rowH(Setting s) {
        return (s != BIND && !s.isBool && !s.isText && !s.isMode) ? 26 : 20;
    }

    private void drawRow(DrawContext ctx, int mx, int my, Module m, Setting s, int x, int y, int w, int h) {
        boolean hover = in(mx, my, x, y, w, h) && my >= clipY0 && my < clipY1;
        rr(ctx, x, y, x + w, y + h, 2, hover ? 0xFF222222 : 0xFF1A1A1A);
        rr(ctx, x + 1, y + 1, x + w - 1, y + h - 1, 2, hover ? 0xFF151515 : 0xFF101010);
        int right = x + w - 8;

        if (s == BIND) {
            ctx.drawTextWithShadow(textRenderer, "Bind", x + 8, y + 6, 0xFFFFFFFF);
            String v = listening == m ? "press a key..." : keyName(m.key);
            ctx.drawTextWithShadow(textRenderer, v, right - textRenderer.getWidth(v), y + 6,
                    listening == m ? 0xFFFFFF55 : 0xFF7FA8FF);
            hit(x, y, w, h, true, (a, b2, c) -> listening = m);
            return;
        }

        if (s.isBool) {
            ctx.drawTextWithShadow(textRenderer, s.name, x + 8, y + 6, 0xFFFFFFFF);
            toggleSwitch(ctx, right - 22, y + 5, s.getBool());
            hit(x, y, w, h, true, (a, b2, c) -> s.toggle());
        } else if (s.isMode) {
            ctx.drawTextWithShadow(textRenderer, s.name, x + 8, y + 6, 0xFFFFFFFF);
            String v = s.modeName();
            ctx.drawTextWithShadow(textRenderer, v, right - textRenderer.getWidth(v), y + 6, PURPLE);
            hit(x, y, w, h, true, (a, b2, c) -> s.cycle());
        } else if (s.isText) {
            ctx.drawTextWithShadow(textRenderer, s.name, x + 8, y + 6, 0xFFFFFFFF);
            String v = s.text + (editing == s ? "_" : "");
            ctx.drawTextWithShadow(textRenderer, v, right - textRenderer.getWidth(v), y + 6, 0xFFDDDDDD);
            hit(x, y, w, h, true, (a, b2, c) -> editing = s);
        } else {
            String v = s.step % 1 == 0 ? String.format(Locale.ROOT, "%.0f", s.value)
                    : String.format(Locale.ROOT, "%.1f", s.value);
            int vr = right;
            if (s.group != null) {
                ctx.fill(right - 12, y + 3, right, y + 13, 0xFFFFFFFF);
                ctx.fill(right - 11, y + 4, right - 1, y + 12, 0xFF000000 | m.color(s.group));
                vr = right - 16;
            }
            ctx.drawTextWithShadow(textRenderer, s.name, x + 8, y + 4, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, v, vr - textRenderer.getWidth(v), y + 4, 0xFFDDDDDD);
            int bx0 = x + 8, bw = w - 16;
            double f = (s.value - s.min) / (s.max - s.min);
            rr(ctx, bx0, y + 17, bx0 + bw, y + 21, 1, 0xFF2A2A2A);
            rr(ctx, bx0, y + 17, bx0 + Math.max(2, (int) (bw * f)), y + 21, 1, PURPLE);
            ctx.fill(bx0 + (int) (bw * f) - 1, y + 15, bx0 + (int) (bw * f) + 2, y + 23, 0xFFFFFFFF);
            hit(x, y, w, h, true, (a, b2, c) -> {
                dragging = s;
                dragX = bx0;
                dragW = bw;
                applyDrag(a);
            });
        }
    }

    // ---------------- wejscie ----------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!introDone()) return false;
        editing = null;
        searching = false;
        listening = null;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!in(mx, my, h.x(), h.y(), h.w(), h.h())) continue;
            if (h.clip() && (my < clipY0 || my >= clipY1)) continue;
            h.c().on(mx, my, button);
            ModuleManager.save();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) { applyDrag(mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) { dragging = null; ModuleManager.save(); }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - vAmount * 16));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE
                    || keyCode == GLFW.GLFW_KEY_DELETE) listening.key = -1;
            else listening.key = keyCode;
            listening.keyDown = true;
            listening = null;
            ModuleManager.save();
            return true;
        }
        if (editing != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !editing.text.isEmpty())
                editing.text = editing.text.substring(0, editing.text.length() - 1);
            else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editing = null;
                ModuleManager.save();
            }
            return true;
        }
        if (searching) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty())
                search = search.substring(0, search.length() - 1);
            else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) searching = false;
            scroll = 0;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && openModule != null) { openModule = null; scroll = 0; return true; }
        if (keyCode == GLFW.GLFW_KEY_P) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr < 32) return super.charTyped(chr, modifiers);
        if (editing != null) {
            if (editing.text.length() < 16) editing.text += chr;
            return true;
        }
        if (searching) {
            if (search.length() < 16) search += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void removed() {
        ModuleManager.save();
    }
}
