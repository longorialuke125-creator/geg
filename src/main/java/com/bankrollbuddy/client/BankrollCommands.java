package com.bankrollbuddy.client;

import com.bankrollbuddy.core.Calc;
import com.bankrollbuddy.core.AmountParser;
import com.bankrollbuddy.core.Session;
import com.bankrollbuddy.core.Settings;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/** /bankroll ... client-side commands. */
final class BankrollCommands {
	private interface Flag {
		void set(boolean value);
	}

	private BankrollCommands() {
	}

	static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommands.literal("bankroll")
			.executes(BankrollCommands::stats)
			.then(ClientCommands.literal("stats").executes(BankrollCommands::stats))
			.then(ClientCommands.literal("help").executes(BankrollCommands::help))
			.then(ClientCommands.literal("win")
				.then(ClientCommands.argument("amount", StringArgumentType.word()).executes(c -> record(c, true))))
			.then(ClientCommands.literal("loss")
				.then(ClientCommands.argument("amount", StringArgumentType.word()).executes(c -> record(c, false))))
			.then(ClientCommands.literal("undo").executes(BankrollCommands::undo))
			.then(ClientCommands.literal("reset").executes(BankrollCommands::reset))
			.then(ClientCommands.literal("start")
				.then(ClientCommands.argument("amount", StringArgumentType.word()).executes(BankrollCommands::start)))
			.then(limit("stoploss", true))
			.then(limit("takeprofit", false))
			.then(toggle("hud", v -> Store.settings.hud = v))
			.then(toggle("track", v -> Store.settings.autoTrack = v))
			.then(toggle("debug", v -> Store.settings.debug = v))
			.then(ClientCommands.literal("kelly")
				.then(ClientCommands.argument("winPercent", DoubleArgumentType.doubleArg(0.01, 99.99))
					.then(ClientCommands.argument("netOdds", DoubleArgumentType.doubleArg(0.01, 1000))
						.executes(BankrollCommands::kelly))))
			.then(ClientCommands.literal("martingale")
				.then(ClientCommands.argument("base", StringArgumentType.word())
					.then(ClientCommands.argument("multiplier", DoubleArgumentType.doubleArg(1, 100))
						.then(ClientCommands.argument("losses", IntegerArgumentType.integer(1, 30))
							.executes(BankrollCommands::martingale))))));
	}

	// ---- helpers ----

	private static void reply(CommandContext<FabricClientCommandSource> c, String text) {
		c.getSource().sendFeedback(Component.literal(text));
	}

	private static int fail(CommandContext<FabricClientCommandSource> c, String text) {
		c.getSource().sendError(Component.literal(text));
		return 0;
	}

	private static double amount(CommandContext<FabricClientCommandSource> c, String name) {
		return AmountParser.parse(StringArgumentType.getString(c, name));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> toggle(String name, Flag flag) {
		return ClientCommands.literal(name)
			.then(ClientCommands.literal("on").executes(c -> {
				flag.set(true);
				Store.save();
				reply(c, name + " on");
				return 1;
			}))
			.then(ClientCommands.literal("off").executes(c -> {
				flag.set(false);
				Store.save();
				reply(c, name + " off");
				return 1;
			}));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> limit(String name, boolean stop) {
		return ClientCommands.literal(name)
			.then(ClientCommands.literal("off").executes(c -> {
				setLimit(stop, 0);
				reply(c, name + " off");
				return 1;
			}))
			.then(ClientCommands.argument("amount", StringArgumentType.word()).executes(c -> {
				double v = amount(c, "amount");
				if (Double.isNaN(v) || v <= 0) {
					return fail(c, "Enter an amount like 50k or 2.5M, or 'off'.");
				}
				setLimit(stop, v);
				reply(c, name + " set to " + AmountParser.money(v) + " of session net");
				return 1;
			}));
	}

	private static void setLimit(boolean stop, double value) {
		if (stop) {
			Store.settings.stopLoss = value;
		} else {
			Store.settings.takeProfit = value;
		}
		Tracker.resetAlerts();
		Store.save();
	}

	// ---- commands ----

	private static int stats(CommandContext<FabricClientCommandSource> c) {
		Session s = Store.session;
		Settings st = Store.settings;

		reply(c, "Session net: " + AmountParser.signedMoney(s.net()) + "  (" + s.wins() + "W / " + s.losses() + "L, "
			+ Math.round(s.winRate() * 100) + "% wins)");

		int streak = s.streak();
		reply(c, "Streak: " + (streak > 0 ? streak + " wins" : streak < 0 ? -streak + " losses" : "none yet"));
		reply(c, "Won " + AmountParser.money(s.totalWon()) + "  |  Lost " + AmountParser.money(s.totalLost()));
		reply(c, "Best win " + AmountParser.money(s.bestWin()) + "  |  Worst loss " + AmountParser.money(s.worstLoss()));

		if (st.startBankroll > 0) {
			reply(c, "Bankroll: " + AmountParser.money(Store.bankroll()) + " (started " + AmountParser.money(st.startBankroll) + ")");
		}
		if (st.stopLoss > 0 || st.takeProfit > 0) {
			reply(c, "Stop-loss: " + (st.stopLoss > 0 ? AmountParser.money(st.stopLoss) : "off")
				+ "  |  Take-profit: " + (st.takeProfit > 0 ? AmountParser.money(st.takeProfit) : "off"));
		}
		return 1;
	}

	private static int help(CommandContext<FabricClientCommandSource> c) {
		reply(c, "Press B (rebind in Controls) to open the Bankroll Buddy menu");
		reply(c, "/bankroll  - session stats");
		reply(c, "/bankroll win|loss <amount>  - add a result by hand (5k, 2.5M ok)");
		reply(c, "/bankroll undo | reset");
		reply(c, "/bankroll start <amount>  - set starting bankroll");
		reply(c, "/bankroll stoploss|takeprofit <amount|off>");
		reply(c, "/bankroll hud|track|debug on|off");
		reply(c, "/bankroll kelly <win%> <netOdds>  - bet sizing (netOdds 1 = even money)");
		reply(c, "/bankroll martingale <base> <multiplier> <losses>  - what a loss ladder costs");
		return 1;
	}

	private static int record(CommandContext<FabricClientCommandSource> c, boolean win) {
		double v = amount(c, "amount");
		if (Double.isNaN(v) || v <= 0) {
			return fail(c, "Enter an amount like 5000, 5k or 2.5M.");
		}
		if (win) {
			Store.session.win(v);
		} else {
			Store.session.loss(v);
		}
		Store.save();
		Tracker.checkAlerts();
		reply(c, (win ? "Win " : "Loss ") + AmountParser.money(v) + " recorded. Net " + AmountParser.signedMoney(Store.session.net()));
		return 1;
	}

	private static int undo(CommandContext<FabricClientCommandSource> c) {
		if (!Store.session.undo()) {
			return fail(c, "Nothing to undo.");
		}
		Store.save();
		reply(c, "Removed last result. Net " + AmountParser.signedMoney(Store.session.net()));
		return 1;
	}

	private static int reset(CommandContext<FabricClientCommandSource> c) {
		Store.session.clear();
		Tracker.resetAlerts();
		Store.save();
		reply(c, "Session reset.");
		return 1;
	}

	private static int start(CommandContext<FabricClientCommandSource> c) {
		double v = amount(c, "amount");
		if (Double.isNaN(v) || v <= 0) {
			return fail(c, "Enter an amount like 100k or 2.5M.");
		}
		Store.settings.startBankroll = v;
		Store.save();
		reply(c, "Starting bankroll set to " + AmountParser.money(v));
		return 1;
	}

	private static int kelly(CommandContext<FabricClientCommandSource> c) {
		double p = DoubleArgumentType.getDouble(c, "winPercent") / 100.0;
		double odds = DoubleArgumentType.getDouble(c, "netOdds");
		double ev = Calc.expectedValue(p, odds);
		double f = Calc.kelly(p, odds);

		reply(c, String.format(Locale.ROOT, "Expected value: %+.2f%% of each bet, on average", ev * 100));
		if (Double.isNaN(f) || f <= 0) {
			reply(c, "No edge: Kelly says bet nothing. Over time this bet is expected to lose money.");
			return 1;
		}

		double half = f / 2;
		double bankroll = Store.bankroll();
		String fullText = String.format(Locale.ROOT, "Full Kelly: %.1f%% of bankroll", f * 100);
		String halfText = String.format(Locale.ROOT, "Half Kelly: %.1f%% of bankroll", half * 100);
		if (bankroll > 0) {
			fullText += " (" + AmountParser.money(bankroll * f) + ")";
			halfText += " (" + AmountParser.money(bankroll * half) + ")";
		}
		reply(c, fullText);
		reply(c, halfText + "  <- usually the safer pick");
		return 1;
	}

	private static int martingale(CommandContext<FabricClientCommandSource> c) {
		double base = amount(c, "base");
		double mult = DoubleArgumentType.getDouble(c, "multiplier");
		int losses = IntegerArgumentType.getInteger(c, "losses");
		if (Double.isNaN(base) || base <= 0) {
			return fail(c, "Enter a base bet like 10k.");
		}

		List<Calc.Step> steps = Calc.martingale(base, mult, losses);
		int shown = Math.min(steps.size(), 10);
		for (int i = 0; i < shown; i++) {
			Calc.Step step = steps.get(i);
			reply(c, "Loss " + step.number() + ": bet " + AmountParser.money(step.bet())
				+ "  (total at risk " + AmountParser.money(step.totalRisked()) + ")");
		}
		if (steps.size() > shown) {
			Calc.Step last = steps.get(steps.size() - 1);
			reply(c, "... Loss " + last.number() + ": bet " + AmountParser.money(last.bet())
				+ "  (total at risk " + AmountParser.money(last.totalRisked()) + ")");
		}

		double bankroll = Store.bankroll();
		if (bankroll > 0) {
			reply(c, "Your bankroll " + AmountParser.money(bankroll) + " covers "
				+ Calc.survivableLosses(bankroll, base, mult) + " straight losses on this ladder.");
		} else {
			reply(c, "Tip: /bankroll start <amount> lets this show how many losses you can cover.");
		}
		return 1;
	}
}
