package ru.pwteam.pwrideperspective.mixin.client;

import ru.pwteam.pwrideperspective.client.CameraTransitionController;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setPosition(Vec3 pos);
	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Inject(method = "setup", at = @At("TAIL"))
	private void mountCinematicCamera$applyOffset(
		BlockGetter area,
		Entity focusedEntity,
		boolean thirdPerson,
		boolean inverseView,
		float tickDelta,
		CallbackInfo ci
	) {
		if (!(focusedEntity instanceof LocalPlayer player)) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		boolean moveCamera = CameraTransitionController.INSTANCE.shouldMoveCamera(client, player);
		boolean overrideFirstPersonLook = CameraTransitionController.INSTANCE.shouldOverrideFirstPersonLook(client, player);
		if (!moveCamera && !overrideFirstPersonLook) {
			return;
		}

		if (overrideFirstPersonLook) {
			float aimPitch = CameraTransitionController.INSTANCE.getAimPitch(tickDelta, player.getViewXRot(tickDelta));
			float aimYaw = CameraTransitionController.INSTANCE.getAimYaw(tickDelta, player.getViewYRot(tickDelta));
			this.setRotation(aimYaw, aimPitch);
			return;
		}

		float cameraPitch = CameraTransitionController.INSTANCE.getCameraPitch(tickDelta, player.getViewXRot(tickDelta));
		float cameraYaw = CameraTransitionController.INSTANCE.getCameraYaw(tickDelta, player.getViewYRot(tickDelta));

		Vec3 offset = CameraTransitionController.INSTANCE.getOffset(
			tickDelta,
			cameraPitch,
			cameraYaw
		);
		offset = offset.add(CameraTransitionController.INSTANCE.getManualThirdPersonOffset(client, player, cameraPitch, cameraYaw));

		Vec3 eyePos = player.getEyePosition(tickDelta);
		if (offset.lengthSqr() <= 0.000001D) {
			this.setPosition(eyePos);
			this.setRotation(cameraYaw, cameraPitch);
			return;
		}

		Vec3 targetPos = eyePos.add(offset);
		Vec3 direction = targetPos.subtract(eyePos).normalize();

		HitResult hit = player.level().clip(
			new ClipContext(
				eyePos,
				targetPos,
				ClipContext.Block.VISUAL,
				ClipContext.Fluid.NONE,
				player
			)
		);

		if (hit.getType() != HitResult.Type.MISS) {
			targetPos = hit.getLocation().subtract(direction.scale(0.15D));
		}

		this.setPosition(targetPos);
		if (CameraTransitionController.INSTANCE.shouldLookAtHead(tickDelta)) {
			Vec3 toHead = eyePos.subtract(targetPos);
			double horizontal = Math.sqrt(toHead.x * toHead.x + toHead.z * toHead.z);
			float lookYaw = (float)(Math.toDegrees(Math.atan2(toHead.z, toHead.x)) - 90.0D);
			float lookPitch = (float)(-Math.toDegrees(Math.atan2(toHead.y, horizontal)));
			this.setRotation(lookYaw, lookPitch);
		} else {
			this.setRotation(cameraYaw, cameraPitch);
		}
	}
}
