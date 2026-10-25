package com.bankrollbuddy.client;

import com.bankrollbuddy.core.Alerts;
import com.bankrollbuddy.core.AmountParser;
import com.bankrollbuddy.core.Session;
import com.bankrollbuddy.core.Settings;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Small session panel in the corner of the screen. */
final class Hud {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREEN = 0xFF55FF55;
	private static final int RED = 0xFFFF5555;
	private static final int GRAY = 0xFFAAAAAA;

	private record Line(String text, int color) {
	}

	private Hud() {
	}

	static void extract(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
		Settings s = Store.settings;
		if (!s.hud) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		Session session = Store.session;

		double net = session.net();
		int netColor = net > 0 ? GREEN : net < 0 ? RED : WHITE;

		List<Line> lines = new ArrayList<>();
		lines.add(new Line("Net " + AmountParser.signedMoney(net), netColor));
		lines.add(new Line(session.wins() + "W / " + session.losses() + "L  (" + Math.round(session.winRate() * 100) + "%)", WHITE));

		int streak = session.streak();
		if (streak != 0) {
			lines.add(new Line(streak > 0 ? streak + " win streak" : -streak + " loss streak", streak > 0 ? GREEN : RED));
		}
		if (s.startBankroll > 0) {
			lines.add(new Line("Bankroll " + AmountParser.money(Store.bankroll()), GRAY));
		}

		Alerts.Kind alert = Alerts.check(session, s);
		if (alert == Alerts.Kind.STOP_LOSS) {
			lines.add(new Line("STOP-LOSS HIT", RED));
		} else if (alert == Alerts.Kind.TAKE_PROFIT) {
			lines.add(new Line("TARGET HIT", GREEN));
		}

		int padding = 3;
		int lineHeight = 10;
		int width = 0;
		for (Line line : lines) {
			width = Math.max(width, font.width(line.text()));
		}
		int x = s.hudX;
		int y = s.hudY;

		// x1, y1, x2, y2, ARGB color
		graphics.fill(x - padding, y - padding, x + width + padding, y + lines.size() * lineHeight - 1 + padding, 0x90000000);
		for (int i = 0; i < lines.size(); i++) {
			Line line = lines.get(i);
			// font, text, x, y, ARGB color, shadow
			graphics.text(font, line.text(), x, y + i * lineHeight, line.color(), true);
		}
	}
}
