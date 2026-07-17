package com.example.autobow;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.BowItem;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class AutoBowClient implements ClientModInitializer {

    private static KeyBinding toggleKey;  // M -> ac/kapat
    private static KeyBinding upKey;      // J -> germe suresi +10ms
    private static KeyBinding downKey;    // K -> germe suresi -10ms

    private boolean active = false;

    // JS'teki Time.sleep(150) ve Time.sleep(50) ile ayni
    private int chargeMs = 150;
    private static final int RESTMS = 50;

    private boolean charging = false;
    private long phaseStart = 0;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autobow.toggle", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M, "category.autobow"));

        upKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autobow.up", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J, "category.autobow"));

        downKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autobow.down", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K, "category.autobow"));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        while (toggleKey.wasPressed()) {
            active = !active;
            setUse(client, false);
            charging = false;
            msg(client, active ? "§e[FastBow] §aAKTIF §7(" + chargeMs + "ms)"
                               : "§e[FastBow] §cKAPALI");
        }

        while (upKey.wasPressed()) {
            chargeMs = Math.min(1000, chargeMs + 10);
            msg(client, "§e[FastBow] Germe: §f" + chargeMs + "ms");
        }

        while (downKey.wasPressed()) {
            chargeMs = Math.max(100, chargeMs - 10);
            msg(client, "§e[FastBow] Germe: §f" + chargeMs + "ms");
        }

        if (!active) return;

        // Menu acikken veya elde yay yokken dur (JS'teki mainHand kontrolu)
        boolean bowInHand = client.player.getMainHandStack().getItem() instanceof BowItem;
        if (client.currentScreen != null || !bowInHand) {
            setUse(client, false);
            charging = false;
            return;
        }

        long now = System.currentTimeMillis();

        if (!charging) {
            // dinlenme fazi bitti mi?
            if (now - phaseStart >= RESTMS) {
                setUse(client, true);   // yayi germeye basla
                charging = true;
                phaseStart = now;
            }
        } else {
            // germe suresi doldu mu?
            if (now - phaseStart >= chargeMs) {
                setUse(client, false);  // birak -> ok gider
                charging = false;
                phaseStart = now;
            }
        }
    }

    private void setUse(MinecraftClient client, boolean state) {
        client.options.useKey.setPressed(state);
    }

    private void msg(MinecraftClient client, String s) {
        if (client.player != null) client.player.sendMessage(Text.literal(s), true);
    }
}
