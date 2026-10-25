package com.bankrollbuddy.core;

/** User settings, saved as JSON. Public fields so Gson can read and write them. */
public final class Settings {
	public boolean hud = true;
	public boolean autoTrack = true;
	public boolean debug = false;

	/** Starting bankroll for this session, 0 = not set. */
	public double startBankroll = 0;

	/** Alert when session net falls to -stopLoss. 0 = off. */
	public double stopLoss = 0;

	/** Alert when session net rises to +takeProfit. 0 = off. */
	public double takeProfit = 0;

	public int hudX = 6;
	public int hudY = 6;

	public String inRegex = ChatMatcher.DEFAULT_IN;
	public String outRegex = ChatMatcher.DEFAULT_OUT;
}
