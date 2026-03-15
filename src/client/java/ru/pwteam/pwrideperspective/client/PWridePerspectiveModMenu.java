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
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			PWridePerspectiveConfig config = PWridePerspectiveConfigManager.get();
			ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.literal("PWrideperspective - Настройки"));
			builder.setSavingRunnable(PWridePerspectiveConfigManager::save);

			ConfigEntryBuilder entries = builder.entryBuilder();
			ConfigCategory general = builder.getOrCreateCategory(Component.literal("Общее"));
			ConfigCategory items = builder.getOrCreateCategory(Component.literal("Предметы и длительность"));
			ConfigCategory binds = builder.getOrCreateCategory(Component.literal("Бинды"));

			general.addEntry(entries.startBooleanToggle(Component.literal("Мод включен"), config.modEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> config.modEnabled = value)
				.build());

			addItemEntries(entries, items, "Арбалет", () -> config.crossbowEnabled, v -> config.crossbowEnabled = v, () -> config.crossbowSeconds, v -> config.crossbowSeconds = v);
			addItemEntries(entries, items, "Снежок", () -> config.snowballEnabled, v -> config.snowballEnabled = v, () -> config.snowballSeconds, v -> config.snowballSeconds = v);
			addItemEntries(entries, items, "Яйцо", () -> config.eggEnabled, v -> config.eggEnabled = v, () -> config.eggSeconds, v -> config.eggSeconds = v);
			addItemEntries(entries, items, "Заряд ветра", () -> config.windChargeEnabled, v -> config.windChargeEnabled = v, () -> config.windChargeSeconds, v -> config.windChargeSeconds = v);
			addItemEntries(entries, items, "Лук", () -> config.bowEnabled, v -> config.bowEnabled = v, () -> config.bowSeconds, v -> config.bowSeconds = v);
			addItemEntries(entries, items, "Трезубец", () -> config.tridentEnabled, v -> config.tridentEnabled = v, () -> config.tridentSeconds, v -> config.tridentSeconds = v);
			addItemEntries(entries, items, "Меч", () -> config.swordEnabled, v -> config.swordEnabled = v, () -> config.swordSeconds, v -> config.swordSeconds = v);
			addItemEntries(entries, items, "Булава", () -> config.maceEnabled, v -> config.maceEnabled = v, () -> config.maceSeconds, v -> config.maceSeconds = v);
			addItemEntries(entries, items, "Топор", () -> config.axeEnabled, v -> config.axeEnabled = v, () -> config.axeSeconds, v -> config.axeSeconds = v);

			binds.addEntry(entries.startKeyCodeField(Component.literal("Модификатор смещения камеры"), keyFromCode(config.cameraAdjustModifierKey))
				.setDefaultValue(keyFromCode(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT))
				.setKeySaveConsumer(value -> config.cameraAdjustModifierKey = value.getValue())
				.build());
			binds.addEntry(entries.startKeyCodeField(Component.literal("Смещение камеры влево"), keyFromCode(config.cameraAdjustLeftKey))
				.setDefaultValue(keyFromCode(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT))
				.setKeySaveConsumer(value -> config.cameraAdjustLeftKey = value.getValue())
				.build());
			binds.addEntry(entries.startKeyCodeField(Component.literal("Смещение камеры вправо"), keyFromCode(config.cameraAdjustRightKey))
				.setDefaultValue(keyFromCode(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT))
				.setKeySaveConsumer(value -> config.cameraAdjustRightKey = value.getValue())
				.build());
			binds.addEntry(entries.startKeyCodeField(Component.literal("Смещение камеры вверх"), keyFromCode(config.cameraAdjustUpKey))
				.setDefaultValue(keyFromCode(org.lwjgl.glfw.GLFW.GLFW_KEY_UP))
				.setKeySaveConsumer(value -> config.cameraAdjustUpKey = value.getValue())
				.build());
			binds.addEntry(entries.startKeyCodeField(Component.literal("Смещение камеры вниз"), keyFromCode(config.cameraAdjustDownKey))
				.setDefaultValue(keyFromCode(org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN))
				.setKeySaveConsumer(value -> config.cameraAdjustDownKey = value.getValue())
				.build());
			binds.addEntry(entries.startKeyCodeField(Component.literal("Приблизить камеру"), keyFromCode(config.cameraZoomInKey))
				.setDefaultValue(InputConstants.UNKNOWN)
				.setKeySaveConsumer(value -> config.cameraZoomInKey = value.getValue())
				.build());
			binds.addEntry(entries.startKeyCodeField(Component.literal("Отдалить камеру"), keyFromCode(config.cameraZoomOutKey))
				.setDefaultValue(InputConstants.UNKNOWN)
				.setKeySaveConsumer(value -> config.cameraZoomOutKey = value.getValue())
				.build());

			return builder.build();
		};
	}

	private static InputConstants.Key keyFromCode(int keyCode) {
		if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) {
			return InputConstants.UNKNOWN;
		}
		return InputConstants.Type.KEYSYM.getOrCreate(keyCode);
	}

	private static void addItemEntries(
		ConfigEntryBuilder entries,
		ConfigCategory category,
		String itemName,
		java.util.function.Supplier<Boolean> enabledSupplier,
		java.util.function.Consumer<Boolean> enabledSaver,
		java.util.function.Supplier<Double> secondsSupplier,
		java.util.function.Consumer<Double> secondsSaver
	) {
		category.addEntry(entries.startBooleanToggle(Component.literal(itemName + ": боевой режим"), enabledSupplier.get())
			.setDefaultValue(true)
			.setSaveConsumer(enabledSaver)
			.build());
		category.addEntry(entries.startDoubleField(Component.literal(itemName + ": длительность (сек)"), secondsSupplier.get())
			.setDefaultValue(0.0D)
			.setMin(0.0D)
			.setMax(60.0D)
			.setSaveConsumer(secondsSaver)
			.build());
	}
}
