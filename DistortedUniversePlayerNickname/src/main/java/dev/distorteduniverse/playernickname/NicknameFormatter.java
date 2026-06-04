package dev.distorteduniverse.playernickname;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class NicknameFormatter {
    private final MiniMessage miniMessage = MiniMessage.builder().strict(true).build();
    private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();

    public FormatResult format(String rawNickname, NicknameSettings.ValidationSettings validation) {
        if (rawNickname == null || rawNickname.isBlank()) {
            return FormatResult.failure("Nickname cannot be blank.");
        }
        if (!validation.allowSpaces() && rawNickname.contains(" ")) {
            return FormatResult.failure("Nickname cannot contain spaces.");
        }

        Component component;
        try {
            component = validation.allowMiniMessage() ? miniMessage.deserialize(rawNickname) : Component.text(rawNickname);
        } catch (RuntimeException exception) {
            return FormatResult.failure("Invalid MiniMessage nickname.");
        }

        String visibleText = plainText.serialize(component).trim();
        if (visibleText.length() < validation.minPlainLength()) {
            return FormatResult.failure("Nickname must be at least " + validation.minPlainLength() + " visible characters.");
        }
        if (visibleText.length() > validation.maxPlainLength()) {
            return FormatResult.failure("Nickname must be at most " + validation.maxPlainLength() + " visible characters.");
        }

        return FormatResult.success(component, visibleText);
    }

    public boolean isValid(String rawNickname, NicknameSettings.ValidationSettings validation) {
        return format(rawNickname, validation).success();
    }

    public record FormatResult(boolean success, Component component, String plainText, String message) {
        public static FormatResult success(Component component, String plainText) {
            return new FormatResult(true, component, plainText, "");
        }

        public static FormatResult failure(String message) {
            return new FormatResult(false, Component.empty(), "", message);
        }
    }
}
