package com.bankrollbuddy.client;

import com.bankrollbuddy.core.Alerts;
import com.bankrollbuddy.core.AmountParser;
import com.bankrollbuddy.core.ChatMatcher;
import com.bankrollbuddy.core.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Listens to server messages and records wins/losses. */
final class Tracker {
	static final String PREFIX = "[Bankroll] ";

	private static boolean stopLossFired;
	private static boolean takeProfitFired;

	private Tracker() {
	}

	/**
	 * Only server/system messages are read (the "game" channel), not player chat,
	 * so another player typing "Bob paid you $1M" in chat does not count.
	 */
	static void onGameMessage(Component message, boolean overlay) {
		if (overlay) {
			return;
		}
		Settings s = Store.settings;
		if (!s.autoTrack) {
			return;
		}
		String line = message.getString();
		if (line.startsWith(PREFIX)) {
			return;
		}

		ChatMatcher.Hit hit = Store.matcher.match(line);
		if (hit == null) {
			if (s.debug && line.indexOf('$') >= 0) {
				say("debug, no match: " + line);
			}
			return;
		}

		if (hit.direction() == ChatMatcher.Direction.IN) {
			Store.session.win(hit.amount());
		} else {
			Store.session.loss(hit.amount());
		}
		Store.save();
		checkAlerts();
	}

	static void checkAlerts() {
		double net = Store.session.net();
		Alerts.Kind kind = Alerts.check(Store.session, Store.settings);
		if (kind == Alerts.Kind.STOP_LOSS && !stopLossFired) {
			stopLossFired = true;
			say("Stop-loss reached (net " + AmountParser.signedMoney(net) + "). That's the limit you set. Time to walk away.");
		} else if (kind == Alerts.Kind.TAKE_PROFIT && !takeProfitFired) {
			takeProfitFired = true;
			say("Take-profit reached (net " + AmountParser.signedMoney(net) + "). You hit your target.");
		}
	}

	static void resetAlerts() {
		stopLossFired = false;
		takeProfitFired = false;
	}

	static void say(String text) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.sendSystemMessage(Component.literal(PREFIX + text));
		}
	}
}
