package ru.pwteam.pwrideperspective.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Boat.class)
public abstract class BoatMixin {
	@Inject(method = "clampRotation", at = @At("HEAD"), cancellable = true)
	private void pwrideperspective$allowPassengerFreeLook(Entity passenger, CallbackInfo ci) {
		if (passenger instanceof Player) {
			// Let the player keep the full 360-degree look rotation. This runs on both
			// client and server, so vanilla's boat yaw clamp cannot redirect projectiles.
			ci.cancel();
		}
	}
}
