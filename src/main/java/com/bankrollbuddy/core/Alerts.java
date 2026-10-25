package com.bankrollbuddy.core;

/** Stop-loss / take-profit check. */
public final class Alerts {
	public enum Kind {
		NONE, STOP_LOSS, TAKE_PROFIT
	}

	private Alerts() {
	}

	public static Kind check(Session session, Settings settings) {
		double net = session.net();
		if (settings.stopLoss > 0 && net <= -settings.stopLoss) {
			return Kind.STOP_LOSS;
		}
		if (settings.takeProfit > 0 && net >= settings.takeProfit) {
			return Kind.TAKE_PROFIT;
		}
		return Kind.NONE;
	}
}
