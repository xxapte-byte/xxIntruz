package pl.intruz.modules;

import pl.intruz.module.Category;

/** Logika w mixinach (EntityMixin / PlayerEntityMixin / ClientPlayerEntityMixin). */
public class NoPush extends pl.intruz.module.Module {
    public NoPush() {
        super("NoPush", Category.MOVEMENT);
        addBool("Entities", true);
        addBool("Blocks", true);
        addBool("Fluids", true);
    }
}
