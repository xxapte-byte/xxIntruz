package pl.intruz.module;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import pl.intruz.modules.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();
    /** pozycje paneli GUI (przeciagane myszka), zapisywane w configu */
    public static final Map<Category, int[]> PANELS = new EnumMap<>(Category.class);

    public static void init() {
        MODULES.add(new Sprint());
        MODULES.add(new Fly());
        MODULES.add(new AutoRespawn());
        MODULES.add(new Brightness());
        MODULES.add(new NoBadEffects());
        MODULES.add(new Hud());
        MODULES.add(new Tracers());
        MODULES.add(new Hitboxes());
        MODULES.add(new NameTags());
        MODULES.add(new NoOverlay());
        MODULES.add(new Freecam());
        MODULES.add(new FastPlace());
        MODULES.add(new NoPush());
        MODULES.add(new Incognito());
        load();
    }

    public static Module get(String name) {
        for (Module m : MODULES) if (m.name.equalsIgnoreCase(name)) return m;
        return null;
    }

    public static List<Module> in(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) if (m.category == c) out.add(m);
        return out;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("intruz.json");
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (Module m : MODULES) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.enabled);
                o.addProperty("bind", m.key);
                for (Setting s : m.settings) {
                    if (s.isText) o.addProperty(s.name, s.text);
                    else if (s.isBool) o.addProperty(s.name, s.getBool());
                    else o.addProperty(s.name, s.value);
                }
                root.add(m.name, o);
            }
            JsonObject panels = new JsonObject();
            for (var e : PANELS.entrySet()) {
                JsonArray a = new JsonArray();
                a.add(e.getValue()[0]);
                a.add(e.getValue()[1]);
                panels.add(e.getKey().name(), a);
            }
            root.add("_panels", panels);
            Files.writeString(path(), new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void load() {
        try {
            if (!Files.exists(path())) return;
            JsonObject root = JsonParser.parseString(Files.readString(path())).getAsJsonObject();
            for (Module m : MODULES) {
                if (!root.has(m.name)) continue;
                JsonObject o = root.getAsJsonObject(m.name);
                if (o.has("enabled")) m.enabled = o.get("enabled").getAsBoolean();
                if (o.has("bind")) m.key = o.get("bind").getAsInt();
                for (Setting s : m.settings) {
                    if (!o.has(s.name)) continue;
                    JsonElement e = o.get(s.name);
                    if (s.isText) s.text = e.getAsString();
                    else if (s.isBool) s.value = e.getAsBoolean() ? 1 : 0;
                    else s.value = Math.max(s.min, Math.min(s.max, e.getAsDouble()));
                }
            }
            if (root.has("_panels")) {
                JsonObject panels = root.getAsJsonObject("_panels");
                for (Category c : Category.values()) {
                    if (!panels.has(c.name())) continue;
                    JsonArray a = panels.getAsJsonArray(c.name());
                    PANELS.put(c, new int[]{a.get(0).getAsInt(), a.get(1).getAsInt()});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
