package pl.intruz.module;

public class Setting {
    public final String name;
    public final boolean isBool, isText, isMode;
    public double value, min, max, step;
    public String text = "";
    public String[] modes;
    /** jesli ustawione - to jeden z kanalow R/G/B grupy kolorow o tej nazwie */
    public String group;

    public Setting(String name, boolean def) {
        this.name = name; isBool = true; isText = false; isMode = false; value = def ? 1 : 0;
    }

    public Setting(String name, double def, double min, double max, double step) {
        this.name = name; isBool = false; isText = false; isMode = false;
        value = def; this.min = min; this.max = max; this.step = step;
    }

    public Setting(String name, String def) {
        this.name = name; isBool = false; isText = true; isMode = false; text = def;
    }

    public Setting(String name, String[] modes, int def) {
        this.name = name; isBool = false; isText = false; isMode = true;
        this.modes = modes; value = def; min = 0; max = modes.length - 1; step = 1;
    }

    public boolean getBool() { return value > 0; }
    public void toggle() { value = getBool() ? 0 : 1; }
    public void cycle() { value = ((int) value + 1) % modes.length; }
    public String modeName() { return modes[(int) value]; }
}
