package ru.pwteam.pwrideperspective.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import ru.pwteam.pwrideperspective.config.PWridePerspectiveConfig;
import ru.pwteam.pwrideperspective.config.PWridePerspectiveConfigManager;

public class PWridePerspectiveModMenu implements ModMenuApi {
	private static final String KEY = "pwrideperspective.config.";

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			PWridePerspectiveConfig config = PWridePerspectiveConfigManager.get();
			ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable(KEY + "title"));
			builder.setSavingRunnable(PWridePerspectiveConfigManager::save);

			ConfigEntryBuilder entries = builder.entryBuilder();
			ConfigCategory general = builder.getOrCreateCategory(Component.translatable(KEY + "general"));
			ConfigCategory items = builder.getOrCreateCategory(Component.translatable(KEY + "items"));
			ConfigCategory binds = builder.getOrCreateCategory(Component.translatable(KEY + "bindings"));

			general.addEntry(entries.startBooleanToggle(Component.translatable(KEY + "enabled"), config.modEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> config.modEnabled = value)
				.build());

			addItemEntries(entries, items, "crossbow", () -> config.crossbowEnabled, v -> config.crossbowEnabled = v, () -> config.crossbowSeconds, v -> config.crossbowSeconds = v);
			addItemEntries(entries, items, "snowball", () -> config.snowballEnabled, v -> config.snowballEnabled = v, () -> config.snowballSeconds, v -> config.snowballSeconds = v);
			addItemEntries(entries, items, "egg", () -> config.eggEnabled, v -> config.eggEnabled = v, () -> config.eggSeconds, v -> config.eggSeconds = v);
			addItemEntries(entries, items, "bow", () -> config.bowEnabled, v -> config.bowEnabled = v, () -> config.bowSeconds, v -> config.bowSeconds = v);
			addItemEntries(entries, items, "trident", () -> config.tridentEnabled, v -> config.tridentEnabled = v, () -> config.tridentSeconds, v -> config.tridentSeconds = v);
			addItemEntries(entries, items, "sword", () -> config.swordEnabled, v -> config.swordEnabled = v, () -> config.swordSeconds, v -> config.swordSeconds = v);
			addItemEntries(entries, items, "axe", () -> config.axeEnabled, v -> config.axeEnabled = v, () -> config.axeSeconds, v -> config.axeSeconds = v);

			addKeyEntry(entries, binds, "modifier", config.cameraAdjustModifierKey, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT, v -> config.cameraAdjustModifierKey = v);
			addKeyEntry(entries, binds, "left", config.cameraAdjustLeftKey, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT, v -> config.cameraAdjustLeftKey = v);
			addKeyEntry(entries, binds, "right", config.cameraAdjustRightKey, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, v -> config.cameraAdjustRightKey = v);
			addKeyEntry(entries, binds, "up", config.cameraAdjustUpKey, org.lwjgl.glfw.GLFW.GLFW_KEY_UP, v -> config.cameraAdjustUpKey = v);
			addKeyEntry(entries, binds, "down", config.cameraAdjustDownKey, org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN, v -> config.cameraAdjustDownKey = v);
			addKeyEntry(entries, binds, "zoom_in", config.cameraZoomInKey, org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN, v -> config.cameraZoomInKey = v);
			addKeyEntry(entries, binds, "zoom_out", config.cameraZoomOutKey, org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN, v -> config.cameraZoomOutKey = v);

			return builder.build();
		};
	}

	private static void addKeyEntry(ConfigEntryBuilder entries, ConfigCategory category, String key, int value, int defaultKey, java.util.function.IntConsumer saver) {
		category.addEntry(entries.startKeyCodeField(Component.translatable(KEY + "binding." + key), keyFromCode(value))
			.setDefaultValue(keyFromCode(defaultKey))
			.setKeySaveConsumer(input -> saver.accept(input.getValue()))
			.build());
	}

	private static InputConstants.Key keyFromCode(int keyCode) {
		if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) {
			return InputConstants.UNKNOWN;
		}
		return InputConstants.Type.KEYSYM.getOrCreate(keyCode);
	}

	private static void addItemEntries(ConfigEntryBuilder entries, ConfigCategory category, String itemKey,
		java.util.function.Supplier<Boolean> enabledSupplier, java.util.function.Consumer<Boolean> enabledSaver,
		java.util.function.Supplier<Double> secondsSupplier, java.util.function.Consumer<Double> secondsSaver) {
		String itemLabelKey = KEY + "item." + itemKey;
		category.addEntry(entries.startBooleanToggle(Component.translatable(itemLabelKey + ".combat"), enabledSupplier.get())
			.setDefaultValue(true)
			.setSaveConsumer(enabledSaver)
			.build());
		category.addEntry(entries.startDoubleField(Component.translatable(itemLabelKey + ".duration"), secondsSupplier.get())
			.setDefaultValue(0.0D)
			.setMin(0.0D)
			.setMax(60.0D)
			.setSaveConsumer(secondsSaver)
			.build());
	}
}
