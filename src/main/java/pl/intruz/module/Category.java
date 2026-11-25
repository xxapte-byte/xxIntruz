package pl.intruz.module;

public enum Category {
    WORLD("World"), PLAYER("Player"), MOVEMENT("Movement"), RENDER("Render"), MISC("Misc");

    public final String label;
    Category(String label) { this.label = label; }
}
