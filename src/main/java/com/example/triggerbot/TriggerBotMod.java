package com.example.triggerbot;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

public class TriggerBotMod implements ClientModInitializer {

    private static KeyBinding toggleKey;
    private static boolean botEnabled = false;
    private static long lastAttackTime = 0;
    private static final long COOLDOWN_MS = 5000; // Кулдаун 5 секунд

    @Override
    public void onInitializeClient() {
        // Регистрация кнопки INSERT
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.triggerbot.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_INSERT,
                "category.triggerbot"
        ));

        // Вызывается каждый тик игры (20 раз в секунду)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // Переключение кнопкой INS
            if (toggleKey.wasPressed()) {
                botEnabled = !botEnabled;
                String status = botEnabled ? "§aACTIVATED" : "§cDEACTIVATED";
                client.player.sendMessage(Text.literal("§7[TriggerBot] " + status), true);
            }

            if (!botEnabled) return;

            long currentTime = System.currentTimeMillis();

            // Проверяем 5 секунд задержки
            if (currentTime - lastAttackTime >= COOLDOWN_MS) {
                // Встроенный триггер игры (наведение на хитбокс игрока)
                if (client.targetedEntity instanceof PlayerEntity enemy) {
                    if (enemy.isAlive()) {
                        // 1. Прыгаем
                        client.player.jump();
                        
                        // Рандом для обхода античитов на Realms / Серверах (230-280мс)
                        int randomDelay = ThreadLocalRandom.current().nextInt(230, 281);
                        
                        // 2. В отдельном потоке ждем начала падения для крита
                        new Thread(() -> {
                            try {
                                Thread.sleep(randomDelay);
                                
                                // Удар возвращаем в главный поток игры
                                client.execute(() -> {
                                    if (client.interactionManager != null && client.player != null && client.targetedEntity == enemy) {
                                        client.interactionManager.attackEntity(client.player, enemy);
                                        client.player.swingHand(Hand.MAIN_HAND);
                                        lastAttackTime = System.currentTimeMillis();
                                    }
                                });
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                }
            }
        });
    }
}
