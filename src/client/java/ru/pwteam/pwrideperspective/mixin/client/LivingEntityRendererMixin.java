package ru.pwteam.pwrideperspective.mixin.client;

import ru.pwteam.pwrideperspective.client.CameraTransitionController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Inject(method = "getModelTint", at = @At("RETURN"), cancellable = true)
	private void mountCinematicCamera$applyTransitionAlpha(
		LivingEntityRenderState state,
		CallbackInfoReturnable<Integer> cir
	) {
		if (!(state instanceof PlayerRenderState playerState)) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (!CameraTransitionController.INSTANCE.shouldApplyPlayerAlpha(playerState.id, client)) {
			return;
		}

		int originalColor = cir.getReturnValueI();
		int rgb = originalColor & 0x00FFFFFF;
		int alpha = Math.round(CameraTransitionController.INSTANCE.getPlayerAlpha() * 255.0F) & 0xFF;
		cir.setReturnValue((alpha << 24) | rgb);
	}
}
