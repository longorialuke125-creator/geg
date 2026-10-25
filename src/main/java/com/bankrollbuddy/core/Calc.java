package com.bankrollbuddy.core;

import java.util.ArrayList;
import java.util.List;

/** Bet-sizing math. No Minecraft dependencies. */
public final class Calc {
	private Calc() {
	}

	/** One step of a loss-chasing ladder: the bet, and the total risked up to and including it. */
	public record Step(int number, double bet, double totalRisked) {
	}

	/**
	 * Expected profit per 1 wagered. netOdds is the profit on a win per 1 staked
	 * (1.0 = even money, 0.9 = a win pays 0.9x your stake).
	 */
	public static double expectedValue(double winProb, double netOdds) {
		return winProb * netOdds - (1 - winProb);
	}

	/**
	 * Full Kelly fraction of bankroll to stake. Zero or negative means no edge (do not bet).
	 * Returns NaN for invalid input.
	 */
	public static double kelly(double winProb, double netOdds) {
		if (!(winProb > 0 && winProb < 1) || !(netOdds > 0)) {
			return Double.NaN;
		}
		return winProb - (1 - winProb) / netOdds;
	}

	/** Bets needed if you multiply the stake after every loss, for a given number of straight losses. */
	public static List<Step> martingale(double base, double multiplier, int losses) {
		List<Step> out = new ArrayList<>();
		if (!(base > 0) || !(multiplier >= 1) || losses < 1) {
			return out;
		}
		double bet = base;
		double total = 0;
		for (int i = 1; i <= losses; i++) {
			total += bet;
			out.add(new Step(i, bet, total));
			bet *= multiplier;
		}
		return out;
	}

	/** How many straight losses the bankroll can absorb on that ladder before it can't cover the next bet. */
	public static int survivableLosses(double bankroll, double base, double multiplier) {
		if (!(base > 0) || !(multiplier >= 1) || !(bankroll > 0)) {
			return 0;
		}
		double bet = base;
		double total = 0;
		int n = 0;
		while (n < 200) {
			total += bet;
			if (total > bankroll) {
				break;
			}
			n++;
			bet *= multiplier;
		}
		return n;
	}
}
