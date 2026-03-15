package ru.pwteam.pwrideperspective.mixin.client;

import ru.pwteam.pwrideperspective.client.CameraTransitionController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityTurnMixin {
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void mountCinematicCamera$detachRideCamera(double yawChange, double pitchChange, CallbackInfo ci) {
		if (!((Object)this instanceof LocalPlayer player)) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (CameraTransitionController.INSTANCE.onDetachedMouseTurn(player, client, yawChange, pitchChange)) {
			ci.cancel();
		}
	}
}
