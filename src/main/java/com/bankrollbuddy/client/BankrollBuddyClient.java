package com.bankrollbuddy.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class BankrollBuddyClient implements ClientModInitializer {
	public static final String MOD_ID = "bankrollbuddy";

	private static KeyMapping openMenuKey;

	@Override
	public void onInitializeClient() {
		Store.load();

		// Server/system messages only (not player chat).
		ClientReceiveMessageEvents.GAME.register(Tracker::onGameMessage);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			BankrollCommands.register(dispatcher));

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), Hud::extract);

		// Keybind: opens the settings screen. Default key is B; rebind it under Options > Controls > Key Binds.
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.bankrollbuddy.open_menu",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_B,
			category
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// Drain every queued press so holding the key doesn't stack up screens.
			while (openMenuKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new BankrollScreen());
				}
			}
		});
	}
}
