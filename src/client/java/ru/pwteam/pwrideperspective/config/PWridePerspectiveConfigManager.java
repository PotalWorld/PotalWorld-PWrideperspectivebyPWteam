package ru.pwteam.pwrideperspective.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class PWridePerspectiveConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance()
		.getConfigDir()
		.resolve("pwrideperspective.json");
	private static PWridePerspectiveConfig config = new PWridePerspectiveConfig();

	private PWridePerspectiveConfigManager() {
	}

	public static void load() {
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				PWridePerspectiveConfig loaded = GSON.fromJson(reader, PWridePerspectiveConfig.class);
				if (loaded != null) {
					config = loaded;
				}
			} catch (IOException ignored) {
			}
		}
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException ignored) {
		}
	}

	public static PWridePerspectiveConfig get() {
		return config;
	}
}
