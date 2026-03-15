package ru.pwteam.pwrideperspective.config;

import org.lwjgl.glfw.GLFW;

public class PWridePerspectiveConfig {
	public boolean modEnabled = true;

	public boolean crossbowEnabled = true;
	public double crossbowSeconds = 0.0D;

	public boolean snowballEnabled = true;
	public double snowballSeconds = 0.0D;

	public boolean eggEnabled = true;
	public double eggSeconds = 0.0D;

	public boolean windChargeEnabled = true;
	public double windChargeSeconds = 0.0D;

	public boolean bowEnabled = true;
	public double bowSeconds = 3.0D;

	public boolean tridentEnabled = true;
	public double tridentSeconds = 8.0D;

	public boolean swordEnabled = true;
	public double swordSeconds = 4.0D;

	public boolean maceEnabled = true;
	public double maceSeconds = 4.0D;

	public boolean axeEnabled = true;
	public double axeSeconds = 4.0D;

	public double rideCameraRightOffset = 0.35D;
	public double rideCameraUpOffset = 0.0D;
	public double rideCameraDistanceOffset = 0.0D;

	public int cameraAdjustModifierKey = GLFW.GLFW_KEY_LEFT_ALT;
	public int cameraAdjustLeftKey = GLFW.GLFW_KEY_LEFT;
	public int cameraAdjustRightKey = GLFW.GLFW_KEY_RIGHT;
	public int cameraAdjustUpKey = GLFW.GLFW_KEY_UP;
	public int cameraAdjustDownKey = GLFW.GLFW_KEY_DOWN;
	public int cameraZoomInKey = GLFW.GLFW_KEY_UNKNOWN;
	public int cameraZoomOutKey = GLFW.GLFW_KEY_UNKNOWN;
}
