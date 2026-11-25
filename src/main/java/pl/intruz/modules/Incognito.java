package pl.intruz.modules;

import pl.intruz.module.Category;

/** Zmienia TWOJ nick widoczny u Ciebie (nametag w F5, lista graczy). Tylko po stronie klienta. */
public class Incognito extends pl.intruz.module.Module {
    public Incognito() {
        super("Incognito", Category.MISC);
        addText("Name", "Intruz");
    }
}
