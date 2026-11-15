package com.zonemod;

public enum ZoneMode {
    FULL_TINT("Full Red Tint"),
    BORDER("Red Border Vignette"),
    TINT_BORDER("Light Tint + Heavy Border"),
    PULSE("Pulsing Border"),
    HEARTBEAT("Heartbeat Pulse"),
    HARD_FRAME("Solid Red Frame");

    public final String label;

    ZoneMode(String label) { this.label = label; }

    public ZoneMode next() {
        ZoneMode[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}
