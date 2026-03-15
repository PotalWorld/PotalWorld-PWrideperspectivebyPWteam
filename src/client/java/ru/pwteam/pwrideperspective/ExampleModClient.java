package ru.pwteam.pwrideperspective;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import ru.pwteam.pwrideperspective.client.CameraTransitionController;
import ru.pwteam.pwrideperspective.config.PWridePerspectiveConfigManager;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PWridePerspectiveConfigManager.load();
		ClientTickEvents.START_CLIENT_TICK.register(CameraTransitionController.INSTANCE::tick);
	}
}
