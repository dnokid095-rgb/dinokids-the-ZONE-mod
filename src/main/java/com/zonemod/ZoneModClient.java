package com.zonemod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ZoneModClient implements ClientModInitializer {
    private static final String CATEGORY = "category.zonemod";

    private static KeyBinding toggleKey;
    private static KeyBinding modeKey;
    private static KeyBinding intensityUpKey;
    private static KeyBinding intensityDownKey;

    private static boolean enabled = false;
    private static ZoneMode mode = ZoneMode.BORDER;
    private static float intensity = 0.6f; // 0.1 - 1.0

    private static Path configPath;

    @Override
    public void onInitializeClient() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("zonemod.properties");
        load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zonemod.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY));
        modeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zonemod.mode", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY));
        intensityUpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zonemod.intensity_up", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, CATEGORY));
        intensityDownKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zonemod.intensity_down", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                save();
                msg(client, "Zone: " + (enabled ? "ON" : "OFF") + " (" + mode.label + ")");
            }
            while (modeKey.wasPressed()) {
                mode = mode.next();
                save();
                msg(client, "Zone mode: " + mode.label);
            }
            while (intensityUpKey.wasPressed()) {
                intensity = Math.min(1.0f, intensity + 0.1f);
                save();
                msg(client, "Zone intensity: " + Math.round(intensity * 100) + "%");
            }
            while (intensityDownKey.wasPressed()) {
                intensity = Math.max(0.1f, intensity - 0.1f);
                save();
                msg(client, "Zone intensity: " + Math.round(intensity * 100) + "%");
            }
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            if (enabled) render(context);
        });
    }

    private static void msg(MinecraftClient client, String text) {
        if (client.player != null) client.player.sendMessage(Text.literal(text), true);
    }

    // ---------- Rendering ----------

    private static void render(DrawContext ctx) {
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        float i = intensity;

        switch (mode) {
            case FULL_TINT -> tint(ctx, w, h, 0.45f * i);
            case BORDER -> border(ctx, w, h, 0.85f * i, 0.35f);
            case TINT_BORDER -> {
                tint(ctx, w, h, 0.20f * i);
                border(ctx, w, h, 0.95f * i, 0.40f);
            }
            case PULSE -> {
                float p = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 1000.0 * Math.PI * 1.6);
                float a = (0.35f + 0.65f * p) * i;
                tint(ctx, w, h, 0.10f * a);
                border(ctx, w, h, 0.95f * a, 0.30f + 0.10f * p);
            }
            case HEARTBEAT -> {
                double phase = (System.currentTimeMillis() % 1100L) / 1100.0;
                double b1 = Math.exp(-Math.pow((phase - 0.10) * 11, 2));
                double b2 = 0.7 * Math.exp(-Math.pow((phase - 0.32) * 11, 2));
                float beat = (float) Math.min(1.0, b1 + b2);
                float a = (0.25f + 0.75f * beat) * i;
                tint(ctx, w, h, 0.18f * a);
                border(ctx, w, h, 0.95f * a, 0.33f + 0.07f * beat);
            }
            case HARD_FRAME -> {
                int a = (int) (255 * Math.min(1f, 0.9f * i + 0.1f));
                int c = argb(a, 200, 0, 0);
                int t = Math.max(3, h / 60);
                ctx.fill(0, 0, w, t, c);
                ctx.fill(0, h - t, w, h, c);
                ctx.fill(0, t, t, h - t, c);
                ctx.fill(w - t, t, w, h - t, c);
                border(ctx, w, h, 0.35f * i, 0.18f);
            }
        }
    }

    private static void tint(DrawContext ctx, int w, int h, float alpha) {
        ctx.fill(0, 0, w, h, argb((int) (255 * clamp(alpha)), 190, 0, 0));
    }

    /**
     * Smooth red vignette made of concentric non-overlapping rings.
     * maxAlpha = alpha at the very edge, size = fraction of the screen the fade covers.
     */
    private static void border(DrawContext ctx, int w, int h, float maxAlpha, float size) {
        int depth = (int) (Math.min(w, h) * size);
        int steps = 48;
        int step = Math.max(1, depth / steps);
        steps = Math.max(1, depth / step);

        for (int s = 0; s < steps; s++) {
            float t = s / (float) steps;
            float a = maxAlpha * (1f - t) * (1f - t);
            int col = argb((int) (255 * clamp(a)), 200, 0, 0);
            int in = s * step;
            if (in * 2 >= Math.min(w, h)) break;

            ctx.fill(in, in, w - in, in + step, col);                       // top
            ctx.fill(in, h - in - step, w - in, h - in, col);               // bottom
            ctx.fill(in, in + step, in + step, h - in - step, col);         // left
            ctx.fill(w - in - step, in + step, w - in, h - in - step, col); // right
        }
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    private static int argb(int a, int r, int g, int b) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    // ---------- Config ----------

    private static void load() {
        if (!Files.exists(configPath)) return;
        try (InputStream in = Files.newInputStream(configPath)) {
            Properties p = new Properties();
            p.load(in);
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "false"));
            intensity = Float.parseFloat(p.getProperty("intensity", "0.6"));
            try { mode = ZoneMode.valueOf(p.getProperty("mode", "BORDER")); }
            catch (IllegalArgumentException ignored) {}
        } catch (IOException | NumberFormatException ignored) {}
    }

    private static void save() {
        try (OutputStream out = Files.newOutputStream(configPath)) {
            Properties p = new Properties();
            p.setProperty("enabled", String.valueOf(enabled));
            p.setProperty("mode", mode.name());
            p.setProperty("intensity", String.valueOf(intensity));
            p.store(out, "dinokid509s Zone mod config");
        } catch (IOException ignored) {}
    }
}
