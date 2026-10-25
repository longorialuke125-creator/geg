package com.bankrollbuddy.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Turns a server chat line into a win/loss event.
 * IN  = someone paid you (counted as a win).
 * OUT = you paid someone (counted as a loss).
 *
 * The default patterns are best guesses at the pay message wording. They are configurable in
 * config/bankroll-buddy.json (inRegex / outRegex). Each needs named groups "amount" and, optionally, "player".
 */
public final class ChatMatcher {
	public enum Direction {
		IN, OUT
	}

	public record Hit(Direction direction, String player, double amount) {
	}

	private static final String AMOUNT = "(?<amount>\\d[\\d,]*(?:\\.\\d+)?\\s?[kmbt]?)(?!\\w)";

	public static final String DEFAULT_IN =
		"(?i)(?<!\\w)(?<player>\\w{3,16})\\s+(?:has\\s+)?(?:paid|sent)\\s+you\\s+\\$?\\s?" + AMOUNT;

	public static final String DEFAULT_OUT =
		"(?i)(?<!\\w)you\\s+(?:have\\s+)?(?:paid|sent)\\s+(?<player>\\w{3,16})\\s+\\$?\\s?" + AMOUNT;

	private final Pattern in;
	private final Pattern out;

	public ChatMatcher(String inRegex, String outRegex) {
		this.in = compile(inRegex, DEFAULT_IN);
		this.out = compile(outRegex, DEFAULT_OUT);
	}

	private static Pattern compile(String regex, String fallback) {
		try {
			return Pattern.compile(regex == null || regex.isBlank() ? fallback : regex);
		} catch (PatternSyntaxException e) {
			return Pattern.compile(fallback);
		}
	}

	/** Returns the parsed event, or null if the line is not a pay message. */
	public Hit match(String line) {
		if (line == null || line.isEmpty()) {
			return null;
		}
		Hit hit = tryPattern(in, Direction.IN, line);
		if (hit != null) {
			return hit;
		}
		return tryPattern(out, Direction.OUT, line);
	}

	private static Hit tryPattern(Pattern pattern, Direction direction, String line) {
		Matcher m = pattern.matcher(line);
		if (!m.find()) {
			return null;
		}
		double amount;
		try {
			amount = AmountParser.parse(m.group("amount"));
		} catch (IllegalArgumentException e) {
			return null; // custom regex has no "amount" group
		}
		if (Double.isNaN(amount) || amount <= 0) {
			return null;
		}
		String player;
		try {
			player = m.group("player");
		} catch (IllegalArgumentException e) {
			player = "?";
		}
		return new Hit(direction, player, amount);
	}
}
