package dev.distorteduniverse.fakeplayer;

public record SkinProperty(String value, String signature) {
    public SkinProperty {
        signature = signature == null ? "" : signature;
    }
}
