package pl.intruz.modules;

import pl.intruz.module.Category;

public class Hud extends pl.intruz.module.Module {
    public Hud() {
        super("Hud", Category.RENDER);
        enabled = true;
        addBool("Watermark", true);
        addBool("ArrayList", true);
    }
}
