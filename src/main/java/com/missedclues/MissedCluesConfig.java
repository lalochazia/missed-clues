package com.missedclues;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Clue Tiers")
public interface MissedCluesConfig extends Config
{
	@ConfigItem(
			keyName = "beginnerToggle",
			name = "Show Missed Beginner Clues",
			description = "Do not show simulated beginner clues on receiving a missed clue message.",
			position = 0
	)
	default boolean beginnerToggle() {
		return true;
	}
	@ConfigItem(
			keyName = "easyToggle",
			name = "Show Missed Easy Clues",
			description = "Do not show simulated easy clues on receiving a missed clue message.",
			position = 1
	)
	default boolean easyToggle() {
		return true;
	}
	@ConfigItem(
			keyName = "mediumToggle",
			name = "Show Missed Medium Clues",
			description = "Do not show simulated medium clues on receiving a missed clue message.",
			position = 2
	)
	default boolean mediumToggle() {
		return true;
	}
	@ConfigItem(
			keyName = "hardToggle",
			name = "Show Missed Hard Clues",
			description = "Do not show simulated hard clues on receiving a missed clue message.",
			position = 3
	)
	default boolean hardToggle() {
		return true;
	}
	@ConfigItem(
			keyName = "eliteToggle",
			name = "Show Missed Elite Clues",
			description = "Do not show simulated elite clues on receiving a missed clue message.",
			position = 4
	)
	default boolean eliteToggle() {
		return true;
	}
	@ConfigItem(
			keyName = "masterToggle",
			name = "Show Missed Master Clues",
			description = "Do not show simulated master clues on receiving a missed clue message.",
			position = 5
	)
	default boolean masterToggle() {
		return true;
	}
}
