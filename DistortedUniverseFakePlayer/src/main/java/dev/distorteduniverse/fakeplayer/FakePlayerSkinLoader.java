package dev.distorteduniverse.fakeplayer;

import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FakePlayerSkinLoader {
    private final JavaPlugin plugin;
    private final Map<String, WrappedSignedProperty> skins = new HashMap<>();
    private String defaultSkin = "default";

    public FakePlayerSkinLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(FakePlayerSettings.SkinsSettings settings) {
        skins.clear();
        defaultSkin = settings.defaultSkin();

        File skinsFolder = new File(plugin.getDataFolder(), settings.folder());
        if (!skinsFolder.exists()) {
            skinsFolder.mkdirs();
            createDefaultSkinFile(skinsFolder);
        }

        File[] skinFiles = skinsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (skinFiles != null) {
            for (File file : skinFiles) {
                String skinName = file.getName().replace(".json", "");
                try {
                    String content = readFile(file);
                    WrappedSignedProperty property = parseSkinJson(content);
                    skins.put(skinName, property);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load skin: " + skinName + " - " + e.getMessage());
                }
            }
        }

        if (!skins.containsKey(defaultSkin)) {
            createDefaultSkin(skinsFolder);
        }
    }

    private void createDefaultSkinFile(File skinsFolder) {
        File defaultFile = new File(skinsFolder, "default.json");
        if (!defaultFile.exists()) {
            try {
                defaultFile.createNewFile();
                String defaultJson = """
                    {
                        "name": "default",
                        "value": "eyJ0aW1lc3RhbXAiOjE1MTYyMzkwMjJ9.eyJ4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciLCJ4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciLCJzdHlsZXM9W3siZGlzcGxheSI6ImltYWxlIiwibGlua0lkIjoiMjY2NiIsImltYWdlTGVuZ3RoIjoiNDA4OCIsInZpZXdJbmRleCI6MC41fV0sInRzZXhhbXBsZXMiOnsibGlua0lkIjoiMDAwMCIsImRhdGFiYXNlSWQiOiIzNjM2NDg0MjM1NDY0ODkwNCIsInRzZXgiOiIxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MCIsInRpbmdsZSI6IjEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MCJ9LCJ0aW1lc3RhbXAiOjE1MTYyMzkwMjJ9",
                        "signature": "Example"
                    }
                    """;
                writeFile(defaultFile, defaultJson);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create default skin file: " + e.getMessage());
            }
        }
    }

    private void createDefaultSkin(File skinsFolder) {
        WrappedSignedProperty defaultProperty = WrappedSignedProperty.fromValues(
            "textures",
            "eyJ0aW1lc3RhbXAiOjE1MTYyMzkwMjJ9.eyJ4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciLCJ4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciLCJzdHlsZXM9W3siZGlzcGxheSI6ImltYWxlIiwibGlua0lkIjoiMjY2NiIsImltYWdlTGVuZ3RoIjoiNDA4OCIsInZpZXdJbmRleCI6MC41fV0sInRzZXhhbXBsZXMiOnsibGlua0lkIjoiMDAwMCIsImRhdGFiYXNlSWQiOiIzNjM2NDg0MjM1NDY0ODkwNCIsInRzZXgiOiIxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MCIsInRpbmdsZSI6IjEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MCJ9LCJ0aW1lc3RhbXAiOjE1MTYyMzkwMjJ9",
            "Default"
        );
        skins.put(defaultSkin, defaultProperty);
    }

    private WrappedSignedProperty parseSkinJson(String json) {
        Gson gson = new Gson();
        JsonObject obj = gson.fromJson(json, JsonObject.class);

        String name = obj.has("name") ? obj.get("name").getAsString() : "unknown";
        String value = obj.has("value") ? obj.get("value").getAsString() : "";
        String signature = obj.has("signature") ? obj.get("signature").getAsString() : "";

        return WrappedSignedProperty.fromValues("textures", value, signature.isBlank() ? null : signature);
    }

    public Optional<WrappedSignedProperty> getSkin(String name) {
        return Optional.ofNullable(skins.get(name));
    }

    public Collection<String> getAvailableSkins() {
        return skins.keySet();
    }

    private String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private void writeFile(File file, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(content);
        }
    }
}
