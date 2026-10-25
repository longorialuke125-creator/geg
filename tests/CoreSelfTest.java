import com.bankrollbuddy.core.AmountParser;
import com.bankrollbuddy.core.Alerts;
import com.bankrollbuddy.core.Calc;
import com.bankrollbuddy.core.ChatMatcher;
import com.bankrollbuddy.core.Session;
import com.bankrollbuddy.core.Settings;

import java.util.List;

/**
 * Plain-Java checks for the logic that doesn't touch Minecraft. No test framework needed:
 *   javac -d out src/main/java/com/bankrollbuddy/core/*.java
 *   java -cp out tests/CoreSelfTest.java
 */
public class CoreSelfTest {
	static int checks = 0;
	static int failures = 0;

	static void check(boolean ok, String name) {
		checks++;
		if (!ok) {
			failures++;
			System.out.println("FAIL: " + name);
		}
	}

	static boolean near(double a, double b) {
		return Math.abs(a - b) < 1e-6;
	}

	public static void main(String[] args) {
		amounts();
		formatting();
		matcher();
		session();
		calc();
		alerts();
		System.out.println(checks + " checks, " + failures + " failures");
		if (failures > 0) {
			System.exit(1);
		}
	}

	static void amounts() {
		check(near(AmountParser.parse("1.5k"), 1500), "parse 1.5k");
		check(near(AmountParser.parse("$2,000"), 2000), "parse $2,000");
		check(near(AmountParser.parse("3M"), 3_000_000), "parse 3M");
		check(near(AmountParser.parse("2b"), 2_000_000_000d), "parse 2b");
		check(near(AmountParser.parse(" 5 K "), 5000), "parse with spaces");
		check(near(AmountParser.parse(".5"), 0.5), "parse .5");
		check(Double.isNaN(AmountParser.parse("abc")), "reject abc");
		check(Double.isNaN(AmountParser.parse("1d")), "reject 1d (Java double suffix)");
		check(Double.isNaN(AmountParser.parse("-5")), "reject negative");
		check(Double.isNaN(AmountParser.parse("NaN")), "reject NaN text");
		check(Double.isNaN(AmountParser.parse("")), "reject empty");
		check(Double.isNaN(AmountParser.parse(null)), "reject null");
	}

	static void formatting() {
		check(AmountParser.format(1500).equals("1.5K"), "format 1500");
		check(AmountParser.format(2_000_000).equals("2M"), "format 2M");
		check(AmountParser.format(-1500).equals("-1.5K"), "format -1500");
		check(AmountParser.format(999).equals("999"), "format 999");
		check(AmountParser.format(0).equals("0"), "format 0");
		check(AmountParser.money(1500).equals("$1.5K"), "money");
		check(AmountParser.signedMoney(-2500).equals("-$2.5K"), "signedMoney negative");
		check(AmountParser.signedMoney(1_000_000).equals("+$1M"), "signedMoney positive");
	}

	static void matcher() {
		ChatMatcher m = new ChatMatcher(ChatMatcher.DEFAULT_IN, ChatMatcher.DEFAULT_OUT);

		ChatMatcher.Hit h = m.match("Bob paid you $1,500");
		check(h != null && h.direction() == ChatMatcher.Direction.IN && near(h.amount(), 1500) && h.player().equals("Bob"),
			"IN basic");

		h = m.match("Bob paid you $1,500.");
		check(h != null && near(h.amount(), 1500), "IN trailing period");

		h = m.match("You paid Alice $2.5K");
		check(h != null && h.direction() == ChatMatcher.Direction.OUT && near(h.amount(), 2500) && h.player().equals("Alice"),
			"OUT basic");

		h = m.match("[$] Steve sent you $3M!");
		check(h != null && h.direction() == ChatMatcher.Direction.IN && near(h.amount(), 3_000_000), "IN prefix + suffix");

		h = m.match("Bob paid you $5 bones");
		check(h != null && near(h.amount(), 5), "IN word after amount is not a suffix");

		check(m.match("Bob paid youngster $5") == null, "no match: youngster");
		check(m.match("Bob: paid you $5 lol") == null, "no match: chat-style colon");
		check(m.match("hello world") == null, "no match: unrelated");
		check(m.match("") == null, "no match: empty");
		check(m.match(null) == null, "no match: null");

		ChatMatcher bad = new ChatMatcher("(", "(");
		check(bad.match("Bob paid you $5") != null, "invalid regex falls back to default");

		ChatMatcher noGroup = new ChatMatcher("paid you", "zzzzz");
		check(noGroup.match("Bob paid you $5") == null, "custom regex without amount group is ignored safely");
	}

	static void session() {
		Session s = new Session();
		s.win(100);
		s.loss(50);
		s.loss(25);
		s.win(0);
		s.loss(-10);
		check(s.count() == 3, "ignores zero/negative inputs");
		check(near(s.net(), 25), "net");
		check(s.wins() == 1 && s.losses() == 2, "win/loss counts");
		check(s.streak() == -2, "loss streak");
		check(near(s.bestWin(), 100) && near(s.worstLoss(), 50), "best/worst");
		check(near(s.totalWon(), 100) && near(s.totalLost(), 75), "totals");
		check(near(s.winRate(), 1.0 / 3.0), "win rate");
		check(s.undo() && s.streak() == -1, "undo");
		s.win(5);
		s.win(5);
		check(s.streak() == 2, "win streak");
		s.clear();
		check(s.count() == 0 && s.streak() == 0 && near(s.winRate(), 0) && !s.undo(), "clear / empty");

		Session loaded = new Session(java.util.Arrays.asList(10.0, null, -5.0, 0.0, Double.NaN));
		check(loaded.count() == 2 && near(loaded.net(), 5), "load skips junk values");
	}

	static void calc() {
		check(near(Calc.kelly(0.6, 1.0), 0.2), "kelly 60% even money");
		check(near(Calc.kelly(0.5, 1.0), 0.0), "kelly 50% even money");
		check(Calc.kelly(0.5, 0.9) < 0, "kelly negative with house edge");
		check(Double.isNaN(Calc.kelly(0, 1)) && Double.isNaN(Calc.kelly(1, 1)) && Double.isNaN(Calc.kelly(0.5, 0)),
			"kelly invalid input");
		check(near(Calc.expectedValue(0.5, 1.0), 0.0), "EV fair coin");
		check(near(Calc.expectedValue(0.5, 0.9), -0.05), "EV with 10% haircut");

		List<Calc.Step> steps = Calc.martingale(100, 2, 4);
		check(steps.size() == 4, "martingale length");
		check(near(steps.get(3).bet(), 800) && near(steps.get(3).totalRisked(), 1500), "martingale step 4");
		check(near(steps.get(1).totalRisked(), 300), "martingale step 2 total");
		check(Calc.martingale(0, 2, 4).isEmpty() && Calc.martingale(100, 0.5, 4).isEmpty() && Calc.martingale(100, 2, 0).isEmpty(),
			"martingale invalid input");

		check(Calc.survivableLosses(1000, 100, 2) == 3, "survivable doubling");
		check(Calc.survivableLosses(1000, 100, 1) == 10, "survivable flat");
		check(Calc.survivableLosses(0, 100, 2) == 0, "survivable no bankroll");
	}

	static void alerts() {
		Settings st = new Settings();
		Session s = new Session();
		check(Alerts.check(s, st) == Alerts.Kind.NONE, "alerts off by default");

		st.stopLoss = 100;
		st.takeProfit = 50;
		s.loss(99);
		check(Alerts.check(s, st) == Alerts.Kind.NONE, "just above stop-loss");
		s.loss(1);
		check(Alerts.check(s, st) == Alerts.Kind.STOP_LOSS, "stop-loss at limit");
		s.clear();
		s.win(60);
		check(Alerts.check(s, st) == Alerts.Kind.TAKE_PROFIT, "take-profit");
	}
}
