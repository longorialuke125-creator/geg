package com.bankrollbuddy.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One gambling session. Every result is a signed amount: positive = win, negative = loss.
 * All stats are derived from that list, which keeps undo and saving trivial.
 */
public final class Session {
	private final List<Double> results = new ArrayList<>();

	public Session() {
	}

	public Session(List<Double> saved) {
		for (Double d : saved) {
			if (d != null && d != 0 && !Double.isNaN(d) && !Double.isInfinite(d)) {
				results.add(d);
			}
		}
	}

	public void win(double amount) {
		if (amount > 0) {
			results.add(amount);
		}
	}

	public void loss(double amount) {
		if (amount > 0) {
			results.add(-amount);
		}
	}

	public boolean undo() {
		if (results.isEmpty()) {
			return false;
		}
		results.remove(results.size() - 1);
		return true;
	}

	public void clear() {
		results.clear();
	}

	public List<Double> results() {
		return Collections.unmodifiableList(results);
	}

	public int count() {
		return results.size();
	}

	public double net() {
		double total = 0;
		for (double r : results) {
			total += r;
		}
		return total;
	}

	public int wins() {
		int n = 0;
		for (double r : results) {
			if (r > 0) {
				n++;
			}
		}
		return n;
	}

	public int losses() {
		return results.size() - wins();
	}

	public double totalWon() {
		double t = 0;
		for (double r : results) {
			if (r > 0) {
				t += r;
			}
		}
		return t;
	}

	public double totalLost() {
		double t = 0;
		for (double r : results) {
			if (r < 0) {
				t += -r;
			}
		}
		return t;
	}

	/** Fraction of results that were wins, 0..1. */
	public double winRate() {
		return results.isEmpty() ? 0 : (double) wins() / results.size();
	}

	/** Positive = current win streak length, negative = current loss streak length, 0 = no results. */
	public int streak() {
		if (results.isEmpty()) {
			return 0;
		}
		boolean win = results.get(results.size() - 1) > 0;
		int n = 0;
		for (int i = results.size() - 1; i >= 0; i--) {
			if ((results.get(i) > 0) == win) {
				n++;
			} else {
				break;
			}
		}
		return win ? n : -n;
	}

	public double bestWin() {
		double best = 0;
		for (double r : results) {
			best = Math.max(best, r);
		}
		return best;
	}

	/** Largest single loss as a positive number. */
	public double worstLoss() {
		double worst = 0;
		for (double r : results) {
			worst = Math.max(worst, -r);
		}
		return worst;
	}
}
