package pl.intruz.module;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name;
    public final Category category;
    public boolean enabled;
    public boolean expanded;
    /** kod klawisza GLFW do wl/wyl modulu, -1 = brak */
    public int key = -1;
    public boolean keyDown;
    public final List<Setting> settings = new ArrayList<>();

    protected final MinecraftClient mc = MinecraftClient.getInstance();

    protected Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    protected Setting addBool(String n, boolean def) {
        Setting s = new Setting(n, def); settings.add(s); return s;
    }

    protected Setting addNum(String n, double def, double min, double max, double step) {
        Setting s = new Setting(n, def, min, max, step); settings.add(s); return s;
    }

    protected Setting addText(String n, String def) {
        Setting s = new Setting(n, def); settings.add(s); return s;
    }

    protected Setting addMode(String n, int def, String... modes) {
        Setting s = new Setting(n, modes, def); settings.add(s); return s;
    }

    /** dodaje 3 suwaki: "<prefix> R", "<prefix> G", "<prefix> B" */
    protected void addColor(String prefix, int r, int g, int b) {
        String[] ch = {" R", " G", " B"};
        int[] v = {r, g, b};
        for (int i = 0; i < 3; i++) {
            Setting s = new Setting(prefix + ch[i], v[i], 0, 255, 5);
            s.group = prefix;
            settings.add(s);
        }
    }

    public int color(String prefix) {
        return ((int) num(prefix + " R") << 16) | ((int) num(prefix + " G") << 8) | (int) num(prefix + " B");
    }

    public boolean bool(String n) {
        for (Setting s : settings) if (s.name.equals(n)) return s.getBool();
        return false;
    }

    public double num(String n) {
        for (Setting s : settings) if (s.name.equals(n)) return s.value;
        return 0;
    }

    public int mode(String n) { return (int) num(n); }

    public String text(String n) {
        for (Setting s : settings) if (s.name.equals(n)) return s.text;
        return "";
    }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean e) {
        if (enabled == e) return;
        enabled = e;
        if (e) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick(MinecraftClient mc) {}
    public void onWorldRender(WorldRenderContext ctx) {}
    public void onHud(DrawContext ctx) {}
}
