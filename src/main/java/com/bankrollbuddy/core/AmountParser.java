package com.bankrollbuddy.core;

import java.util.Locale;
import java.util.regex.Pattern;

/** Parses and formats money amounts like "$1,500", "2.5k", "3M". No Minecraft dependencies. */
public final class AmountParser {
	private static final Pattern PLAIN_NUMBER = Pattern.compile("^(\\d+(\\.\\d*)?|\\.\\d+)$");

	private AmountParser() {
	}

	/** Returns the amount, or NaN if the text is not a valid non-negative amount. */
	public static double parse(String raw) {
		if (raw == null) {
			return Double.NaN;
		}
		String s = raw.trim().replace("$", "").replace(",", "").replace("_", "").replace(" ", "").toLowerCase(Locale.ROOT);
		if (s.isEmpty()) {
			return Double.NaN;
		}
		double multiplier = 1;
		char last = s.charAt(s.length() - 1);
		switch (last) {
			case 'k' -> multiplier = 1e3;
			case 'm' -> multiplier = 1e6;
			case 'b' -> multiplier = 1e9;
			case 't' -> multiplier = 1e12;
			default -> {
			}
		}
		if (multiplier != 1) {
			s = s.substring(0, s.length() - 1);
		}
		if (!PLAIN_NUMBER.matcher(s).matches()) {
			return Double.NaN;
		}
		try {
			return Double.parseDouble(s) * multiplier;
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	/** 1500 -> "1.5K", -2000000 -> "-2M". */
	public static String format(double value) {
		double a = Math.abs(value);
		String sign = value < 0 ? "-" : "";
		if (a >= 1e12) {
			return sign + trim(a / 1e12) + "T";
		}
		if (a >= 1e9) {
			return sign + trim(a / 1e9) + "B";
		}
		if (a >= 1e6) {
			return sign + trim(a / 1e6) + "M";
		}
		if (a >= 1e3) {
			return sign + trim(a / 1e3) + "K";
		}
		return sign + trim(a);
	}

	/** "$1.5K" or "-$1.5K". */
	public static String money(double value) {
		return (value < 0 ? "-$" : "$") + format(Math.abs(value));
	}

	/** "+$1.5K", "-$1.5K", or "$0". */
	public static String signedMoney(double value) {
		if (value > 0) {
			return "+" + money(value);
		}
		return money(value);
	}

	private static String trim(double x) {
		String s = String.format(Locale.ROOT, "%.2f", x);
		if (s.contains(".")) {
			s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
		}
		return s;
	}
}
