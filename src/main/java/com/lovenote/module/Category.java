package com.lovenote.module;

public enum Category {
	COMBAT("Combat"), RAGE("Rage"), MOVEMENT("Movement"), PLAYER("Player"), VISUALS("Visuals"), RENDER("Render"),
	HUD("HUD"), MISC("Misc"), SERVER_MODULES("Server Modules"), DONUT("Donut"), HYPIXEL("Hypixel"), SPOTIFY("Spotify"), THEMES("Themes");

	public final String label;

	Category(String label) {
		this.label = label;
	}
}
