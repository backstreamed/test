package com.example.autobow;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class AutoBowClient implements ClientModInitializer {

    private static KeyBinding toggleKey;
    private static boolean active = false;

    // Yayin kac tick cekilecegi (daha yuksek = daha guclu ama daha yavas)
    private static final int CHARGE_TICKS = 6;
    private static final int RELEASE_TICKS = 2;

    private int timer = 0;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autobow.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.autobow"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        while (toggleKey.wasPressed()) {
            active = !active;
            timer = 0;
            client.options.useKey.setPressed(false);
            client.player.sendMessage(
                    Text.literal(active ? "§aAutoBow: ACIK" : "§cAutoBow: KAPALI"), true);
        }

        if (!active) return;

        if (client.currentScreen != null) {
            client.options.useKey.setPressed(false);
            return;
        }

        if (!selectBow(client)) {
            client.options.useKey.setPressed(false);
            return;
        }

        timer++;
        if (timer <= CHARGE_TICKS) {
            client.options.useKey.setPressed(true);
        } else if (timer <= CHARGE_TICKS + RELEASE_TICKS) {
            client.options.useKey.setPressed(false);
        } else {
            timer = 0;
        }
    }

    // Hotbar'da yay varsa ona gecer
    private boolean selectBow(MinecraftClient client) {
        var inv = client.player.getInventory();
        if (inv.getStack(inv.selectedSlot).getItem() instanceof BowItem) return true;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.getItem() instanceof BowItem) {
                inv.selectedSlot = i;
                return true;
            }
        }
        return false;
    }
}
