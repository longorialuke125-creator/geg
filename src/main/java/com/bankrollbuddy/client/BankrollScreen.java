package com.bankrollbuddy.client;

import com.bankrollbuddy.core.AmountParser;
import com.bankrollbuddy.core.Session;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The settings screen opened by the keybind. Buttons only, so it works with no typing. */
final class BankrollScreen extends Screen {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREEN = 0xFF55FF55;
	private static final int RED = 0xFFFF5555;
	private static final int GRAY = 0xFFAAAAAA;

	/** Clicking a limit button steps through these, then back to off. */
	private static final double[] LIMIT_PRESETS = {
		10_000, 50_000, 100_000, 500_000, 1_000_000, 5_000_000, 10_000_000, 50_000_000
	};

	private Button hudButton;
	private Button trackButton;
	private Button stopLossButton;
	private Button takeProfitButton;

	BankrollScreen() {
		super(Component.literal("Bankroll Buddy"));
	}

	@Override
	protected void init() {
		int w = 200;
		int x = this.width / 2 - w / 2;
		int y = 92;
		int step = 24;

		hudButton = Button.builder(hudLabel(), b -> {
			Store.settings.hud = !Store.settings.hud;
			Store.save();
			b.setMessage(hudLabel());
		}).bounds(x, y, w, 20).build();

		trackButton = Button.builder(trackLabel(), b -> {
			Store.settings.autoTrack = !Store.settings.autoTrack;
			Store.save();
			b.setMessage(trackLabel());
		}).bounds(x, y + step, w, 20).build();

		stopLossButton = Button.builder(limitLabel("Stop-loss", Store.settings.stopLoss), b -> {
			Store.settings.stopLoss = nextPreset(Store.settings.stopLoss);
			Tracker.resetAlerts();
			Store.save();
			b.setMessage(limitLabel("Stop-loss", Store.settings.stopLoss));
		}).bounds(x, y + step * 2, w, 20).build();

		takeProfitButton = Button.builder(limitLabel("Take-profit", Store.settings.takeProfit), b -> {
			Store.settings.takeProfit = nextPreset(Store.settings.takeProfit);
			Tracker.resetAlerts();
			Store.save();
			b.setMessage(limitLabel("Take-profit", Store.settings.takeProfit));
		}).bounds(x, y + step * 3, w, 20).build();

		Button undoButton = Button.builder(Component.literal("Undo last"), b -> {
			Store.session.undo();
			Store.save();
		}).bounds(x, y + step * 4, 98, 20).build();

		Button resetButton = Button.builder(Component.literal("Reset session"), b -> {
			Store.session.clear();
			Tracker.resetAlerts();
			Store.save();
		}).bounds(x + 102, y + step * 4, 98, 20).build();

		Button doneButton = Button.builder(Component.literal("Done"), b -> this.onClose())
			.bounds(x, y + step * 5 + 4, w, 20).build();

		addRenderableWidget(hudButton);
		addRenderableWidget(trackButton);
		addRenderableWidget(stopLossButton);
		addRenderableWidget(takeProfitButton);
		addRenderableWidget(undoButton);
		addRenderableWidget(resetButton);
		addRenderableWidget(doneButton);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		Font font = Minecraft.getInstance().font;
		Session s = Store.session;
		double net = s.net();

		drawCentered(graphics, font, "Bankroll Buddy", 18, WHITE);
		drawCentered(graphics, font, "Session net " + AmountParser.signedMoney(net), 36,
			net > 0 ? GREEN : net < 0 ? RED : WHITE);
		drawCentered(graphics, font, s.wins() + "W / " + s.losses() + "L  (" + Math.round(s.winRate() * 100) + "% wins)", 48, WHITE);

		int streak = s.streak();
		String streakText = streak > 0 ? streak + " win streak" : streak < 0 ? -streak + " loss streak" : "no results yet";
		drawCentered(graphics, font, streakText, 60, streak > 0 ? GREEN : streak < 0 ? RED : GRAY);

		if (Store.settings.startBankroll > 0) {
			drawCentered(graphics, font, "Bankroll " + AmountParser.money(Store.bankroll()), 72, GRAY);
		} else {
			drawCentered(graphics, font, "Set a starting bankroll with /bankroll start <amount>", 72, GRAY);
		}
	}

	private void drawCentered(GuiGraphicsExtractor graphics, Font font, String text, int y, int color) {
		// font, text, x, y, ARGB color, shadow
		graphics.text(font, text, this.width / 2 - font.width(text) / 2, y, color, true);
	}

	private static Component hudLabel() {
		return Component.literal("HUD panel: " + (Store.settings.hud ? "ON" : "OFF"));
	}

	private static Component trackLabel() {
		return Component.literal("Auto-tracking: " + (Store.settings.autoTrack ? "ON" : "OFF"));
	}

	private static Component limitLabel(String name, double value) {
		return Component.literal(name + ": " + (value > 0 ? AmountParser.money(value) : "OFF"));
	}

	private static double nextPreset(double current) {
		for (double p : LIMIT_PRESETS) {
			if (p > current) {
				return p;
			}
		}
		return 0;
	}
}
