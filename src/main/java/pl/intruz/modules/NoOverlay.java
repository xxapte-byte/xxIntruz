package pl.intruz.modules;

import pl.intruz.module.Category;

/** Logika w mixinach (InGameOverlayRendererMixin / InGameHudMixin). */
public class NoOverlay extends pl.intruz.module.Module {
    public NoOverlay() {
        super("NoOverlay", Category.RENDER);
        addBool("Fire", true);
        addBool("Water", true);
        addBool("InWall", true);
        addBool("Pumpkin", true);
    }
}
