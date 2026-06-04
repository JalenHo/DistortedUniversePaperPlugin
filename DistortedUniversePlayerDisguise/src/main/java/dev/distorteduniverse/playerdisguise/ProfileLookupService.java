package dev.distorteduniverse.playerdisguise;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class ProfileLookupService {
    private static final Pattern JSON_STRING_FIELD = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern TEXTURES_PROPERTY = Pattern.compile(
        "\\{[^{}]*\"name\"\\s*:\\s*\"textures\"[^{}]*\"value\"\\s*:\\s*\"([^\"]+)\"(?:[^{}]*\"signature\"\\s*:\\s*\"([^\"]+)\")?[^{}]*}"
    );

    private final Plugin plugin;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ProfileLookupService(Plugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<LookupResult> lookup(String rawUsername) {
        String username;
        try {
            username = DisguiseSettingParser.validateUsername(rawUsername);
        } catch (IllegalArgumentException exception) {
            return CompletableFuture.completedFuture(LookupResult.failure(exception.getMessage()));
        }

        PlayerProfile profile = Bukkit.createProfile(username);
        return profile.update()
            .thenCompose(updatedProfile -> {
                LookupResult result = fromProfile(username, updatedProfile);
                if (result.success()) {
                    return CompletableFuture.completedFuture(result);
                }
                return completeWithTextures(username, updatedProfile);
            })
            .exceptionally(exception -> {
                plugin.getLogger().warning("Profile lookup failed for " + username + ": " + exception.getMessage());
                return lookupFromMojang(username);
            });
    }

    private CompletableFuture<LookupResult> completeWithTextures(String sourceName, PlayerProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PlayerProfile profileToComplete = Bukkit.createProfile(profile.getId(), profile.getName());
                profileToComplete.complete(true, true);
                LookupResult result = fromProfile(sourceName, profileToComplete);
                if (result.success()) {
                    return result;
                }
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Full profile texture lookup failed for " + sourceName + ": " + exception.getMessage());
            }
            return lookupFromMojang(sourceName);
        });
    }

    private LookupResult lookupFromMojang(String sourceName) {
        try {
            MojangIdentity identity = lookupMojangIdentity(sourceName);
            if (identity == null) {
                return LookupResult.failure("Could not resolve Minecraft profile: " + sourceName);
            }

            String sessionBody = getJson(
                "https://sessionserver.mojang.com/session/minecraft/profile/"
                    + identity.sourceId().toString().replace("-", "")
                    + "?unsigned=false"
            );
            String textureValue = textureField(sessionBody, 1);
            String textureSignature = textureField(sessionBody, 2);
            if (textureValue == null || textureValue.isBlank()) {
                return LookupResult.failure("Mojang profile has no skin texture property: " + identity.profileName());
            }

            return LookupResult.success(new ResolvedProfile(
                sourceName,
                identity.sourceId(),
                identity.profileName(),
                textureValue,
                textureSignature == null ? "" : textureSignature,
                System.currentTimeMillis()
            ));
        } catch (IOException exception) {
            plugin.getLogger().warning("Mojang profile lookup failed for " + sourceName + ": " + exception.getMessage());
            return LookupResult.failure("Could not resolve Minecraft profile through Mojang: " + sourceName);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return LookupResult.failure("Interrupted while resolving Minecraft profile: " + sourceName);
        }
    }

    private MojangIdentity lookupMojangIdentity(String sourceName) throws IOException, InterruptedException {
        String body = getJson("https://api.mojang.com/users/profiles/minecraft/" + sourceName);
        String rawId = stringField(body, "id");
        String profileName = stringField(body, "name");
        if (rawId == null || profileName == null) {
            return null;
        }
        return new MojangIdentity(parseUndashedUuid(rawId), profileName);
    }

    private String getJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .GET()
            .header("User-Agent", "DistortedUniversePlayerDisguise/0.9.2")
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " from " + url);
        }
        return response.body();
    }

    private static String stringField(String json, String name) {
        Matcher matcher = Pattern.compile(JSON_STRING_FIELD.pattern().formatted(Pattern.quote(name))).matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String textureField(String json, int group) {
        Matcher matcher = TEXTURES_PROPERTY.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(group);
    }

    private static UUID parseUndashedUuid(String rawId) {
        String value = rawId.replace("-", "");
        if (value.length() != 32) {
            throw new IllegalArgumentException("Invalid Mojang UUID: " + rawId);
        }
        return UUID.fromString(
            value.substring(0, 8)
                + "-"
                + value.substring(8, 12)
                + "-"
                + value.substring(12, 16)
                + "-"
                + value.substring(16, 20)
                + "-"
                + value.substring(20)
        );
    }

    private static LookupResult fromProfile(String sourceName, PlayerProfile profile) {
        UUID sourceId = profile.getId();
        String profileName = profile.getName();
        if (sourceId == null || profileName == null || profileName.isBlank()) {
            return LookupResult.failure("Resolved profile is incomplete for: " + sourceName);
        }

        Optional<ProfileProperty> textures = profile.getProperties().stream()
            .filter(property -> property.getName().equals("textures"))
            .findFirst();
        if (textures.isEmpty() || textures.get().getValue().isBlank()) {
            return LookupResult.failure("Resolved profile has no skin texture property: " + profileName);
        }

        ProfileProperty textureProperty = textures.get();
        return LookupResult.success(new ResolvedProfile(
            sourceName,
            sourceId,
            profileName,
            textureProperty.getValue(),
            textureProperty.getSignature() == null ? "" : textureProperty.getSignature(),
            System.currentTimeMillis()
        ));
    }

    public record ResolvedProfile(
        String sourceName,
        UUID sourceId,
        String profileName,
        String textureValue,
        String textureSignature,
        long cachedAtEpochMillis
    ) {
    }

    private record MojangIdentity(UUID sourceId, String profileName) {
    }

    public record LookupResult(boolean success, ResolvedProfile profile, String message) {
        public static LookupResult success(ResolvedProfile profile) {
            return new LookupResult(true, profile, "");
        }

        public static LookupResult failure(String message) {
            return new LookupResult(false, null, message);
        }
    }
}
