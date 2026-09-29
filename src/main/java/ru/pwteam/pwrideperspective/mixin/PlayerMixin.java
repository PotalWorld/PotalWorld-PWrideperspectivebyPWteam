package ru.pwteam.pwrideperspective.mixin;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
	private static final double MAX_SWEEP_MOUNT_SPEED_SQR = 0.0064D;

	@Inject(method = "isSweepAttack", at = @At("RETURN"), cancellable = true)
	private void pwrideperspective$allowMountedSweepAttack(
		boolean fullyCharged,
		boolean criticalHit,
		boolean sprintAttack,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (cir.getReturnValueZ() || !fullyCharged || criticalHit || sprintAttack) {
			return;
		}

		Player player = (Player) (Object) this;
		Entity vehicle = player.getVehicle();
		if (vehicle == null || !vehicle.onGround() || !player.getMainHandItem().is(ItemTags.SWORDS)) {
			return;
		}

		Vec3 mountMovement = vehicle.getDeltaMovement();
		if (mountMovement.horizontalDistanceSqr() <= MAX_SWEEP_MOUNT_SPEED_SQR) {
			cir.setReturnValue(true);
		}
	}
}
