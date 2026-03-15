package ru.pwteam.pwrideperspective.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import ru.pwteam.pwrideperspective.config.PWridePerspectiveConfig;
import ru.pwteam.pwrideperspective.config.PWridePerspectiveConfigManager;

public final class CameraTransitionController {
	public static final CameraTransitionController INSTANCE = new CameraTransitionController();

	private static final float TRANSITION_TICKS = 14.0F;
	private static final float AIM_IN_TICKS = 10.0F;
	private static final float RECENTER_TICKS = 24.0F;
	private static final int IDLE_RECENTER_TICKS = 60;
	private static final int CAMERA_OFFSET_STEP_COOLDOWN_TICKS = 2;
	private static final double CAMERA_OFFSET_STEP = 0.05D;
	private static final double CAMERA_OFFSET_LIMIT = 1.5D;
	private static final double CAMERA_DISTANCE_STEP = 0.2D;
	private static final double CAMERA_DISTANCE_LIMIT = 2.5D;
	private static final double MAX_CAMERA_DISTANCE = 4.0D;

	private static final int MODE_OFF = 0;
	private static final int MODE_MOUNT_OUT = 1;
	private static final int MODE_DISMOUNT_RETURN = 2;
	private static final int MODE_RIDING_RECENTER = 3;
	private static final int MODE_AIM_IN = 4;
	private static final int MODE_AIM_OUT = 5;
	private static final int MODE_DISMOUNT_FIRST_PERSON_SLIDE = 6;
	private static final int MODE_MOUNT_FIRST_PERSON_SLIDE = 7;

	private static final int RETURN_MODE_STRAIGHT = 0;
	private static final int RETURN_MODE_ARC = 1;

	private boolean wasRiding;
	private boolean restoreFirstPerson;
	private int mode;
	private int returnMode;
	private int idleTicks;

	private float progress;
	private float lastProgress;

	private float cameraYaw;
	private float cameraPitch;
	private float lastCameraYaw;
	private float lastCameraPitch;

	private float controlYaw;
	private float lastObservedViewYaw;
	private float lastObservedViewPitch;

	private float recenterFromYaw;
	private float recenterFromPitch;
	private float recenterToYaw;
	private float recenterToPitch;
	private float aimYaw;
	private float aimPitch;
	private float lastAimYaw;
	private float lastAimPitch;
	private Vec3 dismountSlideStartPos = Vec3.ZERO;
	private Vec3 dismountSlideTargetPos = Vec3.ZERO;
	private Vec3 mountSlideStartPos = Vec3.ZERO;
	private Vec3 mountSlideTargetPos = Vec3.ZERO;
	private Vec3 lastGroundEyePos = Vec3.ZERO;
	private Vec3 lastRidingEyePos = Vec3.ZERO;

	private boolean mouseTurnedThisTick;
	private boolean inAimFirstPerson;
	private int crossbowHoldTicks;
	private int snowballHoldTicks;
	private int eggHoldTicks;
	private int windChargeHoldTicks;
	private int bowHoldTicks;
	private int tridentHoldTicks;
	private int swordHoldTicks;
	private int maceHoldTicks;
	private int axeHoldTicks;
	private boolean suppressBowTridentUntilUseRelease;
	private int cameraOffsetAdjustCooldownTicks;

	private CameraTransitionController() {
	}

	public void tick(Minecraft client) {
		PWridePerspectiveConfig config = PWridePerspectiveConfigManager.get();
		if (!config.modEnabled) {
			this.reset();
			return;
		}

		LocalPlayer player = client.player;
		if (player == null) {
			this.reset();
			return;
		}

		this.lastProgress = this.progress;
		this.lastCameraYaw = this.cameraYaw;
		this.lastCameraPitch = this.cameraPitch;
		this.lastAimYaw = this.aimYaw;
		this.lastAimPitch = this.aimPitch;

		float observedYaw = player.getViewYRot(1.0F);
		float observedPitch = player.getViewXRot(1.0F);

		boolean riding = player.getVehicle() != null;
		this.handleRideCameraOffsetHotkeys(client, player, riding);
		if (riding) {
			this.lastRidingEyePos = player.getEyePosition(1.0F);
			if (!this.wasRiding) {
				this.onMount(client, player, observedYaw, observedPitch, player.getYRot());
			}
			this.tickRide(client, player, observedYaw, observedPitch);
		} else {
			this.lastGroundEyePos = player.getEyePosition(1.0F);
			if (this.wasRiding) {
				this.beginDismountReturn(client, player);
			}
			this.tickDismountReturn(client, player);
			this.lastObservedViewYaw = observedYaw;
			this.lastObservedViewPitch = observedPitch;
		}

		this.wasRiding = riding;
		this.mouseTurnedThisTick = false;
	}

	public boolean shouldMoveCamera(Minecraft client, LocalPlayer player) {
		if (!PWridePerspectiveConfigManager.get().modEnabled) {
			return false;
		}
		if (player != client.player) {
			return false;
		}

		CameraType cameraType = client.options.getCameraType();
		if (cameraType == CameraType.FIRST_PERSON) {
			return false;
		}

		if (this.mode == MODE_DISMOUNT_RETURN) {
			return true;
		}
		if (this.mode == MODE_DISMOUNT_FIRST_PERSON_SLIDE) {
			return true;
		}
		if (this.mode == MODE_MOUNT_FIRST_PERSON_SLIDE) {
			return true;
		}

		return player.getVehicle() != null && cameraType == CameraType.THIRD_PERSON_BACK;
	}

	public boolean shouldOverrideFirstPersonLook(Minecraft client, LocalPlayer player) {
		if (!PWridePerspectiveConfigManager.get().modEnabled) {
			return false;
		}
		if (player != client.player) {
			return false;
		}
		return player.getVehicle() != null
			&& this.inAimFirstPerson
			&& client.options.getCameraType() == CameraType.FIRST_PERSON;
	}

	public Vec3 getOffset(float tickDelta, float fallbackViewXRot, float fallbackViewYRot) {
		if (this.mode == MODE_MOUNT_FIRST_PERSON_SLIDE) {
			float raw = Mth.lerp(tickDelta, this.lastProgress, this.progress);
			float t = this.smoothStep(1.0F - raw);
			double x = Mth.lerp(t, this.mountSlideStartPos.x, this.mountSlideTargetPos.x);
			double y = Mth.lerp(t, this.mountSlideStartPos.y, this.mountSlideTargetPos.y);
			double z = Mth.lerp(t, this.mountSlideStartPos.z, this.mountSlideTargetPos.z);
			return new Vec3(x, y, z).subtract(this.mountSlideTargetPos);
		}

		if (this.mode == MODE_DISMOUNT_FIRST_PERSON_SLIDE) {
			float raw = Mth.lerp(tickDelta, this.lastProgress, this.progress);
			float t = this.smoothStep(1.0F - raw);
			double x = Mth.lerp(t, this.dismountSlideStartPos.x, this.dismountSlideTargetPos.x);
			double y = Mth.lerp(t, this.dismountSlideStartPos.y, this.dismountSlideTargetPos.y);
			double z = Mth.lerp(t, this.dismountSlideStartPos.z, this.dismountSlideTargetPos.z);
			return new Vec3(x, y, z).subtract(this.dismountSlideTargetPos);
		}

		float pitch = this.getCameraPitch(tickDelta, fallbackViewXRot);
		float yaw = this.getCameraYaw(tickDelta, fallbackViewYRot);
		Vec3 lookDir = Vec3.directionFromRotation(pitch, yaw);
		double distance = this.getRideCameraDistance() * this.getDistanceFactor(tickDelta);

		if (this.mode == MODE_DISMOUNT_RETURN && this.returnMode == RETURN_MODE_ARC) {
			return this.getDismountArcOffset(tickDelta, lookDir);
		}

		return lookDir.scale(-distance);
	}

	public Vec3 getManualThirdPersonOffset(Minecraft client, LocalPlayer player, float pitch, float yaw) {
		if (player.getVehicle() == null) {
			return Vec3.ZERO;
		}
		if (client.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
			return Vec3.ZERO;
		}
		if (this.mode == MODE_MOUNT_FIRST_PERSON_SLIDE || this.mode == MODE_DISMOUNT_FIRST_PERSON_SLIDE) {
			return Vec3.ZERO;
		}

		PWridePerspectiveConfig cfg = PWridePerspectiveConfigManager.get();
		Vec3 lookDir = Vec3.directionFromRotation(pitch, yaw);
		Vec3 right = lookDir.cross(new Vec3(0.0D, 1.0D, 0.0D));
		if (right.lengthSqr() < 0.0001D) {
			right = new Vec3(1.0D, 0.0D, 0.0D);
		} else {
			right = right.normalize();
		}
		return right.scale(cfg.rideCameraRightOffset).add(0.0D, cfg.rideCameraUpOffset, 0.0D);
	}

	public boolean shouldLookAtHead(float tickDelta) {
		if (this.mode == MODE_DISMOUNT_RETURN && this.returnMode == RETURN_MODE_ARC) {
			float raw = Mth.lerp(tickDelta, this.lastProgress, this.progress);
			return raw > 0.5F;
		}

		return false;
	}

	public float getCameraYaw(float tickDelta, float fallbackViewYRot) {
		if (this.mode == MODE_OFF && !this.wasRiding) {
			return fallbackViewYRot;
		}
		return Mth.rotLerp(tickDelta, this.lastCameraYaw, this.cameraYaw);
	}

	public float getCameraPitch(float tickDelta, float fallbackViewXRot) {
		if (this.mode == MODE_OFF && !this.wasRiding) {
			return fallbackViewXRot;
		}
		return Mth.lerp(tickDelta, this.lastCameraPitch, this.cameraPitch);
	}

	public float getPlayerAlpha() {
		if (this.mode == MODE_MOUNT_FIRST_PERSON_SLIDE || this.mode == MODE_DISMOUNT_FIRST_PERSON_SLIDE) {
			return 0.0F;
		}
		float eased = this.progress * this.progress * (3.0F - 2.0F * this.progress);
		return Mth.clamp(eased, 0.0F, 1.0F);
	}

	public boolean shouldApplyPlayerAlpha(int entityId, Minecraft client) {
		if (!PWridePerspectiveConfigManager.get().modEnabled) {
			return false;
		}
		return client.player != null && client.player.getId() == entityId && this.mode != MODE_OFF;
	}

	public float getAimYaw(float tickDelta, float fallbackViewYRot) {
		if (!this.inAimFirstPerson) {
			return fallbackViewYRot;
		}
		return Mth.rotLerp(tickDelta, this.lastAimYaw, this.aimYaw);
	}

	public float getAimPitch(float tickDelta, float fallbackViewXRot) {
		if (!this.inAimFirstPerson) {
			return fallbackViewXRot;
		}
		return Mth.lerp(tickDelta, this.lastAimPitch, this.aimPitch);
	}

	private void tickRide(Minecraft client, LocalPlayer player, float observedYaw, float observedPitch) {
		this.blockRiptideOnMount(player, client);
		float yawDelta = Mth.wrapDegrees(observedYaw - this.lastObservedViewYaw);
		float pitchDelta = observedPitch - this.lastObservedViewPitch;
		boolean directionalInputDown = this.hasDirectionalInput(client);
		this.updateItemHoldTimers(player, client);

		boolean forceFirstPerson = this.shouldForceFirstPersonByItem(player);
		if (forceFirstPerson) {
			this.idleTicks = 0;
			if (this.inAimFirstPerson && client.options.getCameraType() == CameraType.FIRST_PERSON) {
				this.mode = MODE_OFF;
				this.progress = 0.0F;
				this.lastProgress = 0.0F;
			} else if (client.options.getCameraType() == CameraType.FIRST_PERSON) {
				this.inAimFirstPerson = true;
				this.aimYaw = observedYaw;
				this.lastAimYaw = observedYaw;
				this.aimPitch = observedPitch;
				this.lastAimPitch = observedPitch;
				this.mode = MODE_OFF;
				this.progress = 0.0F;
				this.lastProgress = 0.0F;
			} else if (this.mode != MODE_AIM_IN && this.mode != MODE_MOUNT_FIRST_PERSON_SLIDE) {
				this.mode = MODE_AIM_IN;
				this.progress = 1.0F;
				this.lastProgress = 1.0F;
			}
		} else if (this.inAimFirstPerson && client.options.getCameraType() == CameraType.FIRST_PERSON) {
			client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			this.cameraYaw = observedYaw;
			this.lastCameraYaw = observedYaw;
			this.cameraPitch = observedPitch;
			this.lastCameraPitch = observedPitch;
			this.mode = MODE_AIM_OUT;
			this.progress = 0.0F;
			this.lastProgress = 0.0F;
			this.inAimFirstPerson = false;
		} else if (this.mode == MODE_AIM_IN) {
			this.mode = MODE_AIM_OUT;
		}

		if (this.mode == MODE_MOUNT_OUT) {
			this.progress = Math.min(1.0F, this.progress + (1.0F / TRANSITION_TICKS));
			if (this.progress >= 1.0F) {
				this.mode = MODE_OFF;
			}
		} else if (this.mode == MODE_RIDING_RECENTER) {
			this.progress = Math.min(1.0F, this.progress + (1.0F / RECENTER_TICKS));
			float t = this.smoothStep(this.progress);
			this.cameraYaw = Mth.rotLerp(t, this.recenterFromYaw, this.recenterToYaw);
				this.cameraPitch = Mth.lerp(t, this.recenterFromPitch, this.recenterToPitch);
			if (this.progress >= 1.0F) {
				this.mode = MODE_OFF;
			}
		} else if (this.mode == MODE_AIM_IN) {
			this.progress = Math.max(0.0F, this.progress - (1.0F / AIM_IN_TICKS));
			if (this.progress <= 0.0F) {
				client.options.setCameraType(CameraType.FIRST_PERSON);
				this.inAimFirstPerson = true;
				this.aimYaw = observedYaw;
				this.lastAimYaw = observedYaw;
				this.aimPitch = observedPitch;
				this.lastAimPitch = observedPitch;
				this.mode = MODE_OFF;
				this.progress = 0.0F;
				this.lastProgress = 0.0F;
			}
		} else if (this.mode == MODE_MOUNT_FIRST_PERSON_SLIDE) {
			this.mountSlideTargetPos = player.getEyePosition(1.0F);
			this.progress = Math.max(0.0F, this.progress - (1.0F / AIM_IN_TICKS));
			if (this.progress <= 0.0F) {
				client.options.setCameraType(CameraType.FIRST_PERSON);
				this.inAimFirstPerson = true;
				this.mode = MODE_OFF;
				this.progress = 0.0F;
				this.lastProgress = 0.0F;
			}
		} else if (this.mode == MODE_AIM_OUT) {
			this.progress = Math.min(1.0F, this.progress + (1.0F / TRANSITION_TICKS));
			if (this.progress >= 1.0F) {
				this.mode = MODE_OFF;
			}
		}

		boolean mouseMoved = this.mouseTurnedThisTick
			|| Math.abs(yawDelta) > 0.01F
			|| Math.abs(pitchDelta) > 0.01F;

		boolean hasInput = false;
		if (this.shouldApplyIndependentRideControl(client) && this.mode != MODE_DISMOUNT_RETURN) {
			float baseYaw = client.options.getCameraType() == CameraType.FIRST_PERSON
				? this.aimYaw
				: this.cameraYaw;
			hasInput = this.updateControlDirectionFromKeys(client, baseYaw);
		}

		if (hasInput || directionalInputDown || mouseMoved) {
			this.idleTicks = 0;
			if (this.mode == MODE_RIDING_RECENTER) {
				this.mode = MODE_OFF;
			}
		} else {
			this.idleTicks++;
			if (this.idleTicks >= IDLE_RECENTER_TICKS && this.mode == MODE_OFF) {
				this.beginRideRecenter(player);
			}
		}

		this.lastObservedViewYaw = player.getViewYRot(1.0F);
		this.lastObservedViewPitch = player.getViewXRot(1.0F);
	}

	private void tickDismountReturn(Minecraft client, LocalPlayer player) {
		if (this.mode == MODE_DISMOUNT_FIRST_PERSON_SLIDE) {
			this.dismountSlideTargetPos = player.getEyePosition(1.0F);
			this.progress = Math.max(0.0F, this.progress - (1.0F / TRANSITION_TICKS));
			if (this.progress <= 0.0F) {
				client.options.setCameraType(CameraType.FIRST_PERSON);
				this.reset();
			}
			return;
		}

		if (this.mode != MODE_DISMOUNT_RETURN) {
			return;
		}

		this.progress = Math.max(0.0F, this.progress - (1.0F / TRANSITION_TICKS));
		if (this.progress <= 0.0F) {
			if (this.restoreFirstPerson) {
				client.options.setCameraType(CameraType.FIRST_PERSON);
			}
			this.reset();
		}
	}

	private void onMount(Minecraft client, LocalPlayer player, float observedYaw, float observedPitch, float mountYaw) {
		this.cameraYaw = observedYaw;
		this.lastCameraYaw = observedYaw;
		this.cameraPitch = observedPitch;
		this.lastCameraPitch = observedPitch;
		this.lastObservedViewYaw = observedYaw;
		this.lastObservedViewPitch = observedPitch;
		this.aimYaw = observedYaw;
		this.lastAimYaw = observedYaw;
		this.aimPitch = observedPitch;
		this.lastAimPitch = observedPitch;
		this.controlYaw = mountYaw;
		this.idleTicks = 0;
		this.inAimFirstPerson = false;
		this.crossbowHoldTicks = 0;
		this.snowballHoldTicks = 0;
		this.eggHoldTicks = 0;
		this.windChargeHoldTicks = 0;
		this.bowHoldTicks = 0;
		this.tridentHoldTicks = 0;
		this.swordHoldTicks = 0;
		this.maceHoldTicks = 0;
		this.axeHoldTicks = 0;
		this.suppressBowTridentUntilUseRelease = this.isBowOrTridentInHands(player) && client.options.keyUse.isDown();
		boolean forceInstantGroupOnMount = this.hasInstantFirstPersonGroupInHands(player);
		this.mountSlideStartPos = this.lastGroundEyePos;
		this.mountSlideTargetPos = player.getEyePosition(1.0F);

		if (client.options.getCameraType() == CameraType.FIRST_PERSON) {
			client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			this.restoreFirstPerson = true;
			if (forceInstantGroupOnMount) {
				this.progress = 1.0F;
				this.lastProgress = 1.0F;
				this.mode = MODE_MOUNT_FIRST_PERSON_SLIDE;
			} else {
				this.progress = 0.0F;
				this.lastProgress = 0.0F;
				this.mode = MODE_MOUNT_OUT;
			}
		} else {
			this.restoreFirstPerson = false;
			this.progress = 1.0F;
			this.lastProgress = 1.0F;
			if (forceInstantGroupOnMount) {
				this.mode = MODE_MOUNT_FIRST_PERSON_SLIDE;
			} else {
				this.mode = MODE_OFF;
			}
		}
	}

	private void beginDismountReturn(Minecraft client, LocalPlayer player) {
		boolean shouldSlideToGroundFirstPerson =
			this.inAimFirstPerson
				|| this.hasInstantFirstPersonGroupInHands(player)
				|| this.bowHoldTicks > 0
				|| this.tridentHoldTicks > 0
				|| this.swordHoldTicks > 0
				|| this.maceHoldTicks > 0
				|| this.axeHoldTicks > 0;
		if (shouldSlideToGroundFirstPerson) {
			Vec3 currentCam = client.gameRenderer.getMainCamera().getPosition();
			this.dismountSlideStartPos = this.lastRidingEyePos.lengthSqr() > 0.0001D ? this.lastRidingEyePos : currentCam;
			this.dismountSlideTargetPos = player.getEyePosition(1.0F);
			if (client.options.getCameraType() == CameraType.FIRST_PERSON) {
				client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			}
			this.mode = MODE_DISMOUNT_FIRST_PERSON_SLIDE;
			this.progress = 1.0F;
			this.lastProgress = 1.0F;
			return;
		}

		if (!this.restoreFirstPerson) {
			this.reset();
			return;
		}

		CameraType currentType = client.options.getCameraType();
		if (currentType == CameraType.FIRST_PERSON && this.restoreFirstPerson) {
			this.reset();
			return;
		}
		if (currentType == CameraType.FIRST_PERSON) {
			client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			currentType = CameraType.THIRD_PERSON_BACK;
		}

		this.returnMode = currentType == CameraType.THIRD_PERSON_FRONT ? RETURN_MODE_ARC : RETURN_MODE_STRAIGHT;
		this.mode = MODE_DISMOUNT_RETURN;
		this.progress = 1.0F;
		this.lastProgress = 1.0F;
	}

	private void beginRideRecenter(LocalPlayer player) {
		float targetYaw = this.controlYaw;
		float targetPitch = player.getXRot();
		float delta = Math.abs(Mth.wrapDegrees(targetYaw - this.cameraYaw));
		if (delta < 1.0F) {
			return;
		}

		this.recenterFromYaw = this.cameraYaw;
		this.recenterFromPitch = this.cameraPitch;
		this.recenterToYaw = targetYaw;
		this.recenterToPitch = targetPitch;
		this.mode = MODE_RIDING_RECENTER;
		this.progress = 0.0F;
		this.lastProgress = 0.0F;
	}

	private boolean updateControlDirectionFromKeys(Minecraft client, float baseCameraYaw) {
		int forward = 0;
		int strafe = 0;

		if (client.options.keyUp.isDown()) {
			forward += 1;
		}
		if (client.options.keyDown.isDown()) {
			forward -= 1;
		}
		if (client.options.keyRight.isDown()) {
			strafe += 1;
		}
		if (client.options.keyLeft.isDown()) {
			strafe -= 1;
		}

		if (forward == 0 && strafe == 0) {
			return false;
		}

		float inputAngle = (float) Math.toDegrees(Math.atan2(strafe, forward));
		this.controlYaw = Mth.wrapDegrees(baseCameraYaw + inputAngle);
		return true;
	}

	public void onRideTickHead(LocalPlayer player, Minecraft client) {
		if (!PWridePerspectiveConfigManager.get().modEnabled) {
			return;
		}
		if (player.getVehicle() == null) {
			return;
		}
		if (!this.shouldApplyIndependentRideControl(client)) {
			return;
		}
		this.applyIndependentControl(player, client);
	}

	public void onRideTickTail(LocalPlayer player) {
		// Intentionally empty.
	}

	public void onAiStepInput(LocalPlayer player, Minecraft client) {
		if (!PWridePerspectiveConfigManager.get().modEnabled) {
			return;
		}
		if (this.mode == MODE_DISMOUNT_FIRST_PERSON_SLIDE) {
			this.lockPlayerMovement(player);
			return;
		}
		if (player.getVehicle() == null) {
			return;
		}
		if (!this.shouldApplyIndependentRideControl(client)) {
			return;
		}
		this.applyIndependentControl(player, client);
	}

	public void onAiStepTail(LocalPlayer player, Minecraft client) {
		if (!PWridePerspectiveConfigManager.get().modEnabled) {
			return;
		}
		if (this.mode != MODE_DISMOUNT_FIRST_PERSON_SLIDE) {
			return;
		}
		this.lockPlayerMovement(player);
	}

	private void lockPlayerMovement(LocalPlayer player) {
		player.input.forwardImpulse = 0.0F;
		player.input.leftImpulse = 0.0F;
		player.xxa = 0.0F;
		player.zza = 0.0F;
		player.input.keyPresses = new Input(false, false, false, false, false, false, false);
		player.setSprinting(false);
	}

	public boolean onDetachedMouseTurn(LocalPlayer player, Minecraft client, double yawChange, double pitchChange) {
		if (!PWridePerspectiveConfigManager.get().modEnabled) {
			return false;
		}
		if (player.getVehicle() == null) {
			return false;
		}
		CameraType cameraType = client.options.getCameraType();
		boolean detachedThirdPerson = cameraType == CameraType.THIRD_PERSON_BACK;
		boolean detachedFirstPersonAim = cameraType == CameraType.FIRST_PERSON && this.inAimFirstPerson;
		if (!detachedThirdPerson && !detachedFirstPersonAim) {
			return false;
		}

		if (Math.abs(yawChange) < 0.0001D && Math.abs(pitchChange) < 0.0001D) {
			return true;
		}

		float yawStep = (float)(yawChange * 0.15D);
		float pitchStep = (float)(pitchChange * 0.15D);
		if (detachedFirstPersonAim) {
			this.aimYaw = Mth.wrapDegrees(this.aimYaw + yawStep);
			this.aimPitch = Mth.clamp(this.aimPitch + pitchStep, -89.0F, 89.0F);
		} else {
			this.cameraYaw = Mth.wrapDegrees(this.cameraYaw + yawStep);
			this.cameraPitch = Mth.clamp(this.cameraPitch + pitchStep, -89.0F, 89.0F);
		}
		this.mouseTurnedThisTick = true;
		return true;
	}

	private void applyPlayerLook(LocalPlayer player, float yaw, float pitch) {
		player.setYRot(yaw);
		player.setYHeadRot(yaw);
		player.setYBodyRot(yaw);
		player.setXRot(pitch);
	}

	private void applyControlYaw(LocalPlayer player, float yaw) {
		Entity vehicle = player.getVehicle();
		if (vehicle == null) {
			return;
		}

		vehicle.setYRot(yaw);
		if (vehicle instanceof LivingEntity livingVehicle) {
			livingVehicle.setYHeadRot(yaw);
			livingVehicle.setYBodyRot(yaw);
		}
	}

	private void applyIndependentControl(LocalPlayer player, Minecraft client) {
		CameraType cameraType = client.options.getCameraType();
		boolean firstPersonAimControl = cameraType == CameraType.FIRST_PERSON && this.inAimFirstPerson;
		float baseYaw = firstPersonAimControl ? this.aimYaw : this.cameraYaw;
		boolean hasInput = this.updateControlDirectionFromKeys(client, baseYaw);
		if (firstPersonAimControl) {
			// Visual/player aim follows detached first-person camera, movement yaw stays independent on vehicle.
			this.applyPlayerLook(player, this.aimYaw, this.aimPitch);
		} else {
			this.applyPlayerLook(player, this.controlYaw, player.getXRot());
		}
		this.applyControlYaw(player, this.controlYaw);

		if (hasInput) {
			float forwardImpulse = 1.0F;
			float leftImpulse = 0.0F;

			if (firstPersonAimControl) {
				float yawDiff = Mth.wrapDegrees(this.controlYaw - this.aimYaw);
				float radians = (float)Math.toRadians(yawDiff);
				forwardImpulse = Mth.cos(radians);
				leftImpulse = -Mth.sin(radians);

				// Compensate horse directional penalties (strafe/backward) to keep near-uniform speed.
				float effectiveForward = forwardImpulse > 0.0F ? forwardImpulse : forwardImpulse * 0.25F;
				float effectiveStrafe = leftImpulse * 0.5F;
				float effectiveLen = Mth.sqrt(effectiveForward * effectiveForward + effectiveStrafe * effectiveStrafe);
				if (effectiveLen > 0.0001F) {
					float scale = 1.0F / effectiveLen;
					forwardImpulse *= scale;
					leftImpulse *= scale;
				}
			}

			player.input.forwardImpulse = forwardImpulse;
			player.input.leftImpulse = leftImpulse;
			player.xxa = leftImpulse;
			player.zza = forwardImpulse;
			player.input.keyPresses = new Input(
				forwardImpulse > 0.1F,
				forwardImpulse < -0.1F,
				leftImpulse > 0.1F,
				leftImpulse < -0.1F,
				client.options.keyJump.isDown(),
				client.options.keyShift.isDown(),
				client.options.keySprint.isDown()
			);
		} else {
			player.input.forwardImpulse = 0.0F;
			player.input.leftImpulse = 0.0F;
			player.xxa = 0.0F;
			player.zza = 0.0F;
			player.input.keyPresses = new Input(
				false,
				false,
				false,
				false,
				client.options.keyJump.isDown(),
				client.options.keyShift.isDown(),
				client.options.keySprint.isDown()
			);
		}
	}

	private boolean shouldApplyIndependentRideControl(Minecraft client) {
		CameraType cameraType = client.options.getCameraType();
		if (cameraType == CameraType.THIRD_PERSON_BACK) {
			return true;
		}
		return cameraType == CameraType.FIRST_PERSON && this.inAimFirstPerson;
	}

	private Vec3 getDismountArcOffset(float tickDelta, Vec3 lookDir) {
		double distance = this.getRideCameraDistance();
		float raw = Mth.lerp(tickDelta, this.lastProgress, this.progress);
		Vec3 right = lookDir.cross(new Vec3(0.0D, 1.0D, 0.0D));
		if (right.lengthSqr() < 0.0001D) {
			right = new Vec3(1.0D, 0.0D, 0.0D);
		} else {
			right = right.normalize();
		}

		if (raw > 0.5F) {
			float stage = (1.0F - raw) / 0.5F;
			double angle = stage * Math.PI;
			Vec3 circleDir = lookDir.scale(Math.cos(angle)).add(right.scale(Math.sin(angle)));
			return circleDir.normalize().scale(distance);
		}

		float backStage = raw / 0.5F;
		return lookDir.scale(-distance * backStage);
	}

	private float getDistanceFactor(float tickDelta) {
		float raw = Mth.lerp(tickDelta, this.lastProgress, this.progress);
		float eased = raw * raw * (3.0F - 2.0F * raw);

		if (this.mode == MODE_OFF || this.mode == MODE_RIDING_RECENTER) {
			return 1.0F;
		}

		return eased;
	}

	private double getRideCameraDistance() {
		PWridePerspectiveConfig cfg = PWridePerspectiveConfigManager.get();
		return Mth.clamp(MAX_CAMERA_DISTANCE + cfg.rideCameraDistanceOffset, 1.25D, 8.0D);
	}

	private void updateItemHoldTimers(LocalPlayer player, Minecraft client) {
		PWridePerspectiveConfig cfg = PWridePerspectiveConfigManager.get();
		boolean useDown = client.options.keyUse.isDown();
		if (this.suppressBowTridentUntilUseRelease) {
			if (!useDown) {
				this.suppressBowTridentUntilUseRelease = false;
			}
		}

		boolean crossbowActive = cfg.crossbowEnabled && this.hasItemInHands(player, CrossbowItem.class);
		boolean snowballActive = cfg.snowballEnabled && this.hasItemInHands(player, SnowballItem.class);
		boolean eggActive = cfg.eggEnabled && this.hasItemInHands(player, EggItem.class);
		boolean windChargeActive = cfg.windChargeEnabled && this.hasItemInHands(player, WindChargeItem.class);
		boolean bowActive = cfg.bowEnabled
			&& !this.suppressBowTridentUntilUseRelease
			&& (this.isUsingItemType(player, BowItem.class)
				|| (this.hasItemInHands(player, BowItem.class) && useDown));
		boolean tridentActive = cfg.tridentEnabled
			&& !this.suppressBowTridentUntilUseRelease
			&& (this.isUsingItemType(player, TridentItem.class)
				|| (this.hasItemInHands(player, TridentItem.class) && useDown));
		boolean swordActive = cfg.swordEnabled
			&& client.options.keyAttack.isDown()
			&& this.hasItemInHands(player, SwordItem.class);
		boolean maceActive = cfg.maceEnabled
			&& client.options.keyAttack.isDown()
			&& this.hasItemInHands(player, MaceItem.class);
		boolean axeActive = cfg.axeEnabled
			&& client.options.keyAttack.isDown()
			&& this.hasItemInHands(player, AxeItem.class);

		this.crossbowHoldTicks = this.nextHoldTicks(this.crossbowHoldTicks, crossbowActive, cfg.crossbowSeconds);
		this.snowballHoldTicks = this.nextHoldTicks(this.snowballHoldTicks, snowballActive, cfg.snowballSeconds);
		this.eggHoldTicks = this.nextHoldTicks(this.eggHoldTicks, eggActive, cfg.eggSeconds);
		this.windChargeHoldTicks = this.nextHoldTicks(this.windChargeHoldTicks, windChargeActive, cfg.windChargeSeconds);
		this.bowHoldTicks = this.nextHoldTicks(this.bowHoldTicks, bowActive, cfg.bowSeconds);
		this.tridentHoldTicks = this.nextHoldTicks(this.tridentHoldTicks, tridentActive, cfg.tridentSeconds);
		this.swordHoldTicks = this.nextHoldTicks(this.swordHoldTicks, swordActive, cfg.swordSeconds);
		this.maceHoldTicks = this.nextHoldTicks(this.maceHoldTicks, maceActive, cfg.maceSeconds);
		this.axeHoldTicks = this.nextHoldTicks(this.axeHoldTicks, axeActive, cfg.axeSeconds);
	}

	private boolean shouldForceFirstPersonByItem(LocalPlayer player) {
		return this.crossbowHoldTicks > 0
			|| this.snowballHoldTicks > 0
			|| this.eggHoldTicks > 0
			|| this.windChargeHoldTicks > 0
			|| this.bowHoldTicks > 0
			|| this.tridentHoldTicks > 0
			|| this.swordHoldTicks > 0
			|| this.maceHoldTicks > 0
			|| this.axeHoldTicks > 0;
	}

	private boolean hasInstantFirstPersonGroupInHands(LocalPlayer player) {
		PWridePerspectiveConfig cfg = PWridePerspectiveConfigManager.get();
		boolean mainCrossbow = cfg.crossbowEnabled && player.getMainHandItem().getItem() instanceof CrossbowItem;
		boolean offCrossbow = cfg.crossbowEnabled && player.getOffhandItem().getItem() instanceof CrossbowItem;
		boolean mainSnowball = cfg.snowballEnabled && player.getMainHandItem().getItem() instanceof SnowballItem;
		boolean offSnowball = cfg.snowballEnabled && player.getOffhandItem().getItem() instanceof SnowballItem;
		boolean mainEgg = cfg.eggEnabled && player.getMainHandItem().getItem() instanceof EggItem;
		boolean offEgg = cfg.eggEnabled && player.getOffhandItem().getItem() instanceof EggItem;
		boolean mainWind = cfg.windChargeEnabled && player.getMainHandItem().getItem() instanceof WindChargeItem;
		boolean offWind = cfg.windChargeEnabled && player.getOffhandItem().getItem() instanceof WindChargeItem;
		return mainCrossbow || offCrossbow || mainSnowball || offSnowball || mainEgg || offEgg || mainWind || offWind;
	}

	private boolean isBowOrTridentInHands(LocalPlayer player) {
		return this.hasItemInHands(player, BowItem.class) || this.hasItemInHands(player, TridentItem.class);
	}

	private int nextHoldTicks(int current, boolean active, double seconds) {
		if (active) {
			return this.secondsToTicks(seconds);
		}
		return Math.max(0, current - 1);
	}

	private int secondsToTicks(double seconds) {
		int ticks = (int)Math.round(seconds * 20.0D);
		return Math.max(1, ticks);
	}

	private boolean isUsingItemType(LocalPlayer player, Class<?> itemClass) {
		return player.isUsingItem() && itemClass.isInstance(player.getUseItem().getItem());
	}

	private boolean hasItemInHands(LocalPlayer player, Class<?> itemClass) {
		return itemClass.isInstance(player.getMainHandItem().getItem())
			|| itemClass.isInstance(player.getOffhandItem().getItem());
	}

	private boolean hasDirectionalInput(Minecraft client) {
		return client.options.keyUp.isDown()
			|| client.options.keyDown.isDown()
			|| client.options.keyLeft.isDown()
			|| client.options.keyRight.isDown();
	}

	private void handleRideCameraOffsetHotkeys(Minecraft client, LocalPlayer player, boolean riding) {
		if (client.options.getCameraType() != CameraType.THIRD_PERSON_BACK || !riding) {
			this.cameraOffsetAdjustCooldownTicks = 0;
			return;
		}
		if (client.screen != null) {
			return;
		}

		long window = client.getWindow().getWindow();
		PWridePerspectiveConfig cfg = PWridePerspectiveConfigManager.get();
		boolean modifierDown = this.isKeyDown(window, cfg.cameraAdjustModifierKey);
		boolean zoomInDown = this.isKeyDown(window, cfg.cameraZoomInKey);
		boolean zoomOutDown = this.isKeyDown(window, cfg.cameraZoomOutKey);
		boolean adjustLeft = modifierDown && this.isKeyDown(window, cfg.cameraAdjustLeftKey);
		boolean adjustRight = modifierDown && this.isKeyDown(window, cfg.cameraAdjustRightKey);
		boolean adjustUp = modifierDown && this.isKeyDown(window, cfg.cameraAdjustUpKey);
		boolean adjustDown = modifierDown && this.isKeyDown(window, cfg.cameraAdjustDownKey);

		if (!adjustLeft && !adjustRight && !adjustUp && !adjustDown && !zoomInDown && !zoomOutDown) {
			this.cameraOffsetAdjustCooldownTicks = 0;
			return;
		}

		if (this.cameraOffsetAdjustCooldownTicks > 0) {
			this.cameraOffsetAdjustCooldownTicks--;
			return;
		}

		boolean changed = false;
		if (adjustLeft) {
			cfg.rideCameraRightOffset = Mth.clamp(cfg.rideCameraRightOffset - CAMERA_OFFSET_STEP, -CAMERA_OFFSET_LIMIT, CAMERA_OFFSET_LIMIT);
			changed = true;
		}
		if (adjustRight) {
			cfg.rideCameraRightOffset = Mth.clamp(cfg.rideCameraRightOffset + CAMERA_OFFSET_STEP, -CAMERA_OFFSET_LIMIT, CAMERA_OFFSET_LIMIT);
			changed = true;
		}
		if (adjustUp) {
			cfg.rideCameraUpOffset = Mth.clamp(cfg.rideCameraUpOffset + CAMERA_OFFSET_STEP, -CAMERA_OFFSET_LIMIT, CAMERA_OFFSET_LIMIT);
			changed = true;
		}
		if (adjustDown) {
			cfg.rideCameraUpOffset = Mth.clamp(cfg.rideCameraUpOffset - CAMERA_OFFSET_STEP, -CAMERA_OFFSET_LIMIT, CAMERA_OFFSET_LIMIT);
			changed = true;
		}
		if (zoomInDown) {
			cfg.rideCameraDistanceOffset = Mth.clamp(cfg.rideCameraDistanceOffset - CAMERA_DISTANCE_STEP, -CAMERA_DISTANCE_LIMIT, CAMERA_DISTANCE_LIMIT);
			changed = true;
		}
		if (zoomOutDown) {
			cfg.rideCameraDistanceOffset = Mth.clamp(cfg.rideCameraDistanceOffset + CAMERA_DISTANCE_STEP, -CAMERA_DISTANCE_LIMIT, CAMERA_DISTANCE_LIMIT);
			changed = true;
		}

		if (changed) {
			PWridePerspectiveConfigManager.save();
			this.cameraOffsetAdjustCooldownTicks = CAMERA_OFFSET_STEP_COOLDOWN_TICKS;
		}
	}

	private boolean isKeyDown(long window, int keyCode) {
		return keyCode != GLFW.GLFW_KEY_UNKNOWN && GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
	}

	private void blockRiptideOnMount(LocalPlayer player, Minecraft client) {
		Entity vehicle = player.getVehicle();
		if (vehicle == null) {
			return;
		}

		boolean wet = player.isInWaterOrRain() || vehicle.isInWaterOrRain();
		if (!wet) {
			return;
		}

		if (this.hasRiptideTrident(player.getMainHandItem(), player) || this.hasRiptideTrident(player.getOffhandItem(), player)) {
			if (player.isUsingItem() && player.getUseItem().getItem() instanceof TridentItem) {
				player.stopUsingItem();
			}
			this.tridentHoldTicks = 0;
		}
	}

	private boolean hasRiptideTrident(ItemStack stack, LocalPlayer player) {
		if (!(stack.getItem() instanceof TridentItem)) {
			return false;
		}
		return EnchantmentHelper.getTridentSpinAttackStrength(stack, player) > 0.0F;
	}

	private float smoothStep(float t) {
		return t * t * (3.0F - 2.0F * t);
	}

	private void reset() {
		this.wasRiding = false;
		this.restoreFirstPerson = false;
		this.mode = MODE_OFF;
		this.returnMode = RETURN_MODE_STRAIGHT;
		this.idleTicks = 0;
		this.progress = 0.0F;
		this.lastProgress = 0.0F;
		this.cameraYaw = 0.0F;
		this.cameraPitch = 0.0F;
		this.lastCameraYaw = 0.0F;
		this.lastCameraPitch = 0.0F;
		this.controlYaw = 0.0F;
		this.lastObservedViewYaw = 0.0F;
		this.lastObservedViewPitch = 0.0F;
		this.recenterFromYaw = 0.0F;
		this.recenterFromPitch = 0.0F;
		this.recenterToYaw = 0.0F;
		this.recenterToPitch = 0.0F;
		this.aimYaw = 0.0F;
		this.aimPitch = 0.0F;
		this.lastAimYaw = 0.0F;
		this.lastAimPitch = 0.0F;
		this.dismountSlideStartPos = Vec3.ZERO;
		this.dismountSlideTargetPos = Vec3.ZERO;
		this.mountSlideStartPos = Vec3.ZERO;
		this.mountSlideTargetPos = Vec3.ZERO;
		this.lastGroundEyePos = Vec3.ZERO;
		this.lastRidingEyePos = Vec3.ZERO;
		this.mouseTurnedThisTick = false;
		this.inAimFirstPerson = false;
		this.crossbowHoldTicks = 0;
		this.snowballHoldTicks = 0;
		this.eggHoldTicks = 0;
		this.windChargeHoldTicks = 0;
		this.bowHoldTicks = 0;
		this.tridentHoldTicks = 0;
		this.swordHoldTicks = 0;
		this.maceHoldTicks = 0;
		this.axeHoldTicks = 0;
		this.suppressBowTridentUntilUseRelease = false;
		this.cameraOffsetAdjustCooldownTicks = 0;
	}
}
