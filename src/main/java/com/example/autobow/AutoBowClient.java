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

    private static KeyBinding toggleKey;   // M   -> ac/kapat
    private static KeyBinding upKey;       // .   -> cekim suresi +1
    private static KeyBinding downKey;     // ,   -> cekim suresi -1

    private static boolean active = false;

    private int chargeTicks = 5;   // yay kac tick cekilecek (min 4)
    private int cooldown = 0;      // birakma sonrasi bekleme

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
            cooldown = 0;
            client.options.useKey.setPressed(false);
            msg(client, active ? "§aAutoBow: ACIK §7(" + chargeTicks + " tick)"
                               : "§cAutoBow: KAPALI");
        }

        while (upKey.wasPressed()) {
            chargeTicks = Math.min(20, chargeTicks + 1);
            msg(client, "§eCekim: " + chargeTicks + " tick §7(~" + rate() + " ok/sn)");
        }

        while (downKey.wasPressed()) {
            chargeTicks = Math.max(4, chargeTicks - 1);
            msg(client, "§eCekim: " + chargeTicks + " tick §7(~" + rate() + " ok/sn)");
        }

        if (!active) return;

        if (client.currentScreen != null || !selectBow(client)) {
            client.options.useKey.setPressed(false);
            cooldown = 0;
            return;
        }

        // birakma sonrasi kisa bekleme
        if (cooldown > 0) {
            cooldown--;
            client.options.useKey.setPressed(false);
            return;
        }

        // OYUNUN kendi sayacini oku - kayma olmaz
        int useTime = client.player.isUsingItem() ? client.player.getItemUseTime() : 0;

        if (useTime >= chargeTicks) {
            client.options.useKey.setPressed(false);  // birak -> ok gider
            cooldown = 2;
        } else {
            client.options.useKey.setPressed(true);   // cekmeye devam
        }
    }

    private String rate() {
        return String.format("%.1f", 20.0 / (chargeTicks + 3));
    }

    private void msg(MinecraftClient client, String s) {
        if (client.player != null) client.player.sendMessage(Text.literal(s), true);
    }

    private boolean selectBow(MinecraftClient client) {
        var inv = client.player.getInventory();
        if (inv.getStack(inv.selectedSlot).getItem() instanceof BowItem) return true;
        if (client.player.isUsingItem()) return true;
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
