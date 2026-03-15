package ru.pwteam.pwrideperspective.mixin.client;

import ru.pwteam.pwrideperspective.client.CameraTransitionController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	@Inject(
		method = "rideTick",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/player/AbstractClientPlayer;rideTick()V",
			shift = At.Shift.AFTER
		)
	)
	private void mountCinematicCamera$overrideRideInputHead(CallbackInfo ci) {
		CameraTransitionController.INSTANCE.onRideTickHead((LocalPlayer)(Object)this, Minecraft.getInstance());
	}

	@Inject(method = "rideTick", at = @At("TAIL"))
	private void mountCinematicCamera$overrideRideInputTail(CallbackInfo ci) {
		CameraTransitionController.INSTANCE.onRideTickTail((LocalPlayer)(Object)this);
	}

	@Inject(
		method = "aiStep",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/player/ClientInput;tick()V",
			shift = At.Shift.AFTER
		)
	)
	private void mountCinematicCamera$overrideAiStepInput(CallbackInfo ci) {
		CameraTransitionController.INSTANCE.onAiStepInput((LocalPlayer)(Object)this, Minecraft.getInstance());
	}

	@Inject(method = "aiStep", at = @At("TAIL"))
	private void mountCinematicCamera$restoreAimLookAfterAiStep(CallbackInfo ci) {
		CameraTransitionController.INSTANCE.onAiStepTail((LocalPlayer)(Object)this, Minecraft.getInstance());
	}
}
