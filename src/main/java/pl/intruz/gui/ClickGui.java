package pl.intruz.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import pl.intruz.module.Category;
import pl.intruz.module.Module;
import pl.intruz.module.ModuleManager;
import pl.intruz.module.Setting;

import java.util.List;
import java.util.Locale;

public class ClickGui extends Screen {
    private static final int INTRO_MS = 1400;
    private static final int W = 110, HH = 20, RH = 14, GAP = 12;
    private static final int PURPLE = 0xFFA03CE6, PURPLE_DIM = 0xFF6E2AA0;
    private static final int SLATE = 0xE63A3F55, SLATE_HOVER = 0xF0474D68, SLATE_DARK = 0xF02B2F42;
    private static final Setting BIND = new Setting("Bind", true); // wiersz "Bind: KEY"

    private final long opened = System.currentTimeMillis();
    private Setting dragging, editing;
    private int dragX;
    private Module listening;
    private boolean searching;
    private String search = "";
    private Category dragPanel;
    private int dragOffX, dragOffY;

    private interface Row { void accept(Category c, Module m, Setting s, int x, int y); }

    public ClickGui() { super(Text.literal("Intruz")); }

    @Override
    public boolean shouldPause() { return false; }

    private boolean introDone() { return System.currentTimeMillis() - opened >= INTRO_MS; }

    private static String keyName(int k) {
        if (k <= 0) return "NONE";
        try {
            return InputUtil.fromKeyCode(k, 0).getLocalizedText().getString().toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return "KEY" + k;
        }
    }

    private int[] pos(Category c) {
        return ModuleManager.PANELS.computeIfAbsent(c, k -> new int[]{12 + k.ordinal() * (W + GAP), 30});
    }

    private boolean match(Module m) {
        return search.isEmpty() || m.name.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }

    private void walk(Row row) {
        for (Category c : Category.values()) {
            List<Module> ms = ModuleManager.in(c).stream().filter(this::match).toList();
            if (ms.isEmpty()) continue;
            int[] p = pos(c);
            int x = p[0], y = p[1];
            row.accept(c, null, null, x, y);
            y += HH;
            for (Module m : ms) {
                row.accept(c, m, null, x, y);
                y += RH;
                if (m.expanded) {
                    row.accept(c, m, BIND, x, y);
                    y += RH;
                    for (Setting s : m.settings) {
                        row.accept(c, m, s, x, y);
                        y += RH;
                    }
                }
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xB0000000);
        long t = System.currentTimeMillis() - opened;

        if (t < INTRO_MS) {
            // intro: napis "Intruz", potem GUI
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

        // gorny pasek
        ctx.fill(width / 2 - 70, 4, width / 2 + 70, 18, 0xFF0B0B10);
        ctx.drawCenteredTextWithShadow(textRenderer, "Intruz", width / 2 - 35, 7, PURPLE);
        ctx.drawCenteredTextWithShadow(textRenderer, "Modules", width / 2 + 25, 7, 0xFFFFFFFF);
        ctx.fill(width / 2 + 3, 17, width / 2 + 48, 18, PURPLE);

        walk((c, m, s, x, y) -> {
            int h = m == null ? HH : RH;
            boolean hover = mx >= x && mx < x + W && my >= y && my < y + h;

            if (m == null) {
                ctx.fill(x, y, x + W, y + HH, 0xFF0B0B10);
                var ms = ctx.getMatrices();
                ms.push();
                ms.translate(x + W / 2f, y + 6, 0);
                ms.scale(1.25f, 1.25f, 1f);
                ctx.drawCenteredTextWithShadow(textRenderer, c.label, 0, 0, 0xFFFFFFFF);
                ms.pop();
            } else if (s == null) {
                ctx.fill(x, y, x + W, y + RH, m.enabled ? PURPLE : (hover ? SLATE_HOVER : SLATE));
                ctx.drawCenteredTextWithShadow(textRenderer, m.name, x + W / 2, y + 3, 0xFFFFFFFF);
                if (m.key > 0) {
                    String k = keyName(m.key);
                    if (k.length() > 4) k = k.substring(0, 4);
                    ctx.drawTextWithShadow(textRenderer, "[" + k + "]", x + 3, y + 3, 0xFFC8C8C8);
                }
                ctx.drawTextWithShadow(textRenderer, m.expanded ? "-" : "+", x + W - 9, y + 3, 0xFFC8C8C8);
            } else if (s == BIND) {
                ctx.fill(x, y, x + W, y + RH, hover ? SLATE_HOVER : SLATE_DARK);
                String label = "Bind: " + (listening == m ? "wcisnij klawisz..." : keyName(m.key));
                ctx.drawTextWithShadow(textRenderer, label, x + 6, y + 3, listening == m ? 0xFFFFFF55 : 0xFF7FA8FF);
            } else {
                ctx.fill(x, y, x + W, y + RH, SLATE_DARK);
                String label;
                if (s.isText) {
                    label = s.name + ": " + s.text + (editing == s ? "_" : "");
                } else if (s.isMode) {
                    label = s.name + ": " + s.modeName();
                } else if (s.isBool) {
                    if (s.getBool()) ctx.fill(x, y, x + 3, y + RH, PURPLE);
                    label = s.name + ": " + (s.getBool() ? "ON" : "OFF");
                } else {
                    double f = (s.value - s.min) / (s.max - s.min);
                    ctx.fill(x, y, x + (int) (W * f), y + RH, PURPLE_DIM);
                    label = s.name + ": " + (s.step % 1 == 0 ? String.format(Locale.ROOT, "%.0f", s.value)
                            : String.format(Locale.ROOT, "%.1f", s.value));
                }
                ctx.drawTextWithShadow(textRenderer, label, x + 6, y + 3, 0xFFDDDDDD);
                if (s.group != null) { // podglad koloru
                    ctx.fill(x + W - 15, y + 2, x + W - 3, y + RH - 2, 0xFFFFFFFF);
                    ctx.fill(x + W - 14, y + 3, x + W - 4, y + RH - 3, 0xFF000000 | m.color(s.group));
                }
            }
        });

        // pole Search (lewy dol)
        int sy = height - 24;
        ctx.fill(8, sy, 108, sy + 16, SLATE);
        String st = search.isEmpty() && !searching ? "Search" : search + (searching ? "_" : "");
        ctx.drawCenteredTextWithShadow(textRenderer, st, 58, sy + 4, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                "LPM - wl/wyl | PPM - ustawienia + bind | SPM - szybki bind | przeciagaj naglowki | P - zamknij",
                width / 2, height - 12, 0xFF9A9A9A);
    }

    private void applyDrag(double mx) {
        if (dragging == null) return;
        double f = Math.max(0, Math.min(1, (mx - dragX) / W));
        double v = dragging.min + f * (dragging.max - dragging.min);
        v = Math.round(v / dragging.step) * dragging.step;
        dragging.value = Math.max(dragging.min, Math.min(dragging.max, v));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!introDone()) return false;
        editing = null;
        searching = false;
        listening = null;

        int sy = height - 24;
        if (mx >= 8 && mx < 108 && my >= sy && my < sy + 16) {
            searching = true;
            return true;
        }

        boolean[] done = {false};
        walk((c, m, s, x, y) -> {
            if (done[0]) return;
            int h = m == null ? HH : RH;
            if (mx < x || mx >= x + W || my < y || my >= y + h) return;
            done[0] = true;
            if (m == null) {
                if (button == 0) { dragPanel = c; dragOffX = (int) mx - x; dragOffY = (int) my - y; }
                return;
            }
            if (s == null) {
                if (button == 0) m.toggle();
                else if (button == 1) m.expanded = !m.expanded;
                else if (button == 2) listening = m;
            } else if (s == BIND) {
                listening = m;
            } else if (s.isText) {
                editing = s;
            } else if (s.isMode) {
                s.cycle();
            } else if (s.isBool) {
                s.toggle();
            } else {
                dragging = s;
                dragX = x;
                applyDrag(mx);
            }
            ModuleManager.save();
        });
        return done[0] || super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragPanel != null) {
            int[] p = pos(dragPanel);
            p[0] = Math.max(0, Math.min(width - W, (int) mx - dragOffX));
            p[1] = Math.max(0, Math.min(height - HH, (int) my - dragOffY));
            return true;
        }
        if (dragging != null) { applyDrag(mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragPanel != null || dragging != null) {
            dragPanel = null;
            dragging = null;
            ModuleManager.save();
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening != null) { // ustawianie bindu
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE
                    || keyCode == GLFW.GLFW_KEY_DELETE) listening.key = -1;
            else listening.key = keyCode;
            listening.keyDown = true; // zeby od razu nie przelaczylo
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
            return true;
        }
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
