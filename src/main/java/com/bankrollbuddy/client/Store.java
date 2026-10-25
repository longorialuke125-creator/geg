package com.bankrollbuddy.client;

import com.bankrollbuddy.core.ChatMatcher;
import com.bankrollbuddy.core.Session;
import com.bankrollbuddy.core.Settings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Holds the live settings and session, and saves them to config/bankroll-buddy.json. */
final class Store {
	private static final Logger LOGGER = LoggerFactory.getLogger("bankroll-buddy");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	static Settings settings = new Settings();
	static Session session = new Session();
	static ChatMatcher matcher = new ChatMatcher(settings.inRegex, settings.outRegex);

	private static final class Data {
		Settings settings = new Settings();
		List<Double> session = new ArrayList<>();
	}

	private Store() {
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("bankroll-buddy.json");
	}

	static void load() {
		try {
			Path path = file();
			if (Files.exists(path)) {
				Data data = GSON.fromJson(Files.readString(path), Data.class);
				if (data != null) {
					if (data.settings != null) {
						settings = data.settings;
					}
					if (data.session != null) {
						session = new Session(data.session);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.warn("Could not read bankroll-buddy.json, using defaults", e);
		}
		rebuildMatcher();
		save(); // writes the file with all default fields so they are easy to edit
	}

	static void save() {
		try {
			Data data = new Data();
			data.settings = settings;
			data.session = new ArrayList<>(session.results());
			Files.writeString(file(), GSON.toJson(data));
		} catch (Exception e) {
			LOGGER.warn("Could not save bankroll-buddy.json", e);
		}
	}

	static void rebuildMatcher() {
		matcher = new ChatMatcher(settings.inRegex, settings.outRegex);
	}

	/** Bankroll right now, or 0 if no starting bankroll was set. */
	static double bankroll() {
		return settings.startBankroll > 0 ? settings.startBankroll + session.net() : 0;
	}
}
