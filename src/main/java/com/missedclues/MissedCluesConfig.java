package com.missedclues;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("Clue Tiers")
public interface MissedCluesConfig extends Config
{
	@ConfigSection(
			name = "Clue Tiers",
			description = "Configures how to display each clue tier.",
			position = 0,
			closedByDefault = false
	)
	String SECTION_CLUE_TIERS = "clueTiers";

	@ConfigItem(
			keyName = "beginnerDisplay",
			name = "Beginner Clues",
			description = "Configures how to show missed beginner clues.",
			position = 0,
			section = "clueTiers"
	)
	default DisplayType beginnerDisplay()
	{
		return DisplayType.BOTH;
	}

	@ConfigItem(
			keyName = "easyDisplay",
			name = "Easy Clues",
			description = "Configures how to show missed easy clues.",
			position = 1,
			section = "clueTiers"
	)
	default DisplayType easyDisplay()
	{
		return DisplayType.BOTH;
	}

	@ConfigItem(
			keyName = "mediumDisplay",
			name = "Medium Clues",
			description = "Configures how to show missed medium clues.",
			position = 2,
			section = "clueTiers"
	)
	default DisplayType mediumDisplay()
	{
		return DisplayType.BOTH;
	}

	@ConfigItem(
			keyName = "hardDisplay",
			name = "Hard Clues",
			description = "Configures how to show missed hard clues.",
			position = 3,
			section = "clueTiers"
	)
	default DisplayType hardDisplay()
	{
		return DisplayType.BOTH;
	}

	@ConfigItem(
			keyName = "eliteDisplay",
			name = "Elite Clues",
			description = "Configures how to show missed elite clues.",
			position = 4,
			section = "clueTiers"
	)
	default DisplayType eliteDisplay()
	{
		return DisplayType.BOTH;
	}

	@ConfigItem(
			keyName = "masterDisplay",
			name = "Master Clues",
			description = "Configures how to show missed master clues.",
			position = 5,
			section = "clueTiers"
	)
	default DisplayType masterDisplay()
	{
		return DisplayType.BOTH;
	}

	@ConfigItem(
			keyName = "watsonDisplay",
			name = "Watson Hand-ins",
			description = "Configures how to show Watson hand-ins.",
			position = 6,
			section = "clueTiers"
	)
	default DisplayType watsonDisplay()
	{
		return DisplayType.BOTH;
	}

	@ConfigItem(
			keyName = "missedBeginnerCount",
			name = "Missed Beginner Clues",
			description = "Count of missed Beginner clues",
			hidden = true
	)
	default int missedBeginnerCount()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "missedBeginnerCount",
			name = "Missed Beginner Clues",
			description = "Count of missed Beginner clues",
			hidden = true
	)
	void missedBeginnerCount(int count);


	@ConfigItem(
			keyName = "missedEasyCount",
			name = "Missed Easy Clues",
			description = "Count of missed Easy clues",
			hidden = true
	)
	default int missedEasyCount()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "missedEasyCount",
			name = "Missed Easy Clues",
			description = "Count of missed Easy clues",
			hidden = true
	)
	void missedEasyCount(int count);


	@ConfigItem(
			keyName = "missedMediumCount",
			name = "Missed Medium Clues",
			description = "Count of missed Medium clues",
			hidden = true
	)
	default int missedMediumCount()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "missedMediumCount",
			name = "Missed Medium Clues",
			description = "Count of missed Medium clues",
			hidden = true
	)
	void missedMediumCount(int count);


	@ConfigItem(
			keyName = "missedHardCount",
			name = "Missed Hard Clues",
			description = "Count of missed Hard clues",
			hidden = true
	)
	default int missedHardCount()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "missedHardCount",
			name = "Missed Hard Clues",
			description = "Count of missed Hard clues",
			hidden = true
	)
	void missedHardCount(int count);


	@ConfigItem(
			keyName = "missedEliteCount",
			name = "Missed Elite Clues",
			description = "Count of missed Elite clues",
			hidden = true
	)
	default int missedEliteCount()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "missedEliteCount",
			name = "Missed Elite Clues",
			description = "Count of missed Elite clues",
			hidden = true
	)
	void missedEliteCount(int count);


	@ConfigItem(
			keyName = "missedMasterCount",
			name = "Missed Master Clues",
			description = "Count of missed Master clues",
			hidden = true
	)
	default int missedMasterCount()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "missedMasterCount",
			name = "Missed Master Clues",
			description = "Count of missed Master clues",
			hidden = true
	)
	void missedMasterCount(int count);

	@ConfigItem(
			keyName = "lastMissedValue",
			name = "Last Missed Value",
			description = "Value of the last missed clue",
			hidden = true
	)
	default long lastMissedValue()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "lastMissedValue",
			name = "Last Missed Value",
			description = "Value of the last missed clue",
			hidden = true
	)
	void lastMissedValue(long value);

	@ConfigItem(
			keyName = "lastMissedTier",
			name = "Last Missed Tier",
			description = "Tier of the last missed clue",
			hidden = true
	)
	default String lastMissedTier()
	{
		return "";
	}

	@ConfigItem(
			keyName = "lastMissedTier",
			name = "Last Missed Tier",
			description = "Tier of the last missed clue",
			hidden = true
	)
	void lastMissedTier(String tier);

	@ConfigSection(
			name = "Screenshot",
			description = "Screenshot settings",
			position = 1,
			closedByDefault = false
	)
	String SECTION_SCREENSHOT = "screenshot";

	@ConfigItem(
			keyName = "valuableThreshold",
			name = "Valuable Threshold",
			description = "Takes a screenshot when the clue exceeds this amount (0 for never)",
			position = 0,
			section = "screenshot"
	)
	default int valuableThreshold()
	{
		return 1000000;
	}
	@ConfigItem(
			keyName = "notifyWhenTaken",
			name = "Notify when taken",
			description = "Configures whether or not you are notified when a screenshot has been taken.",
			position = 2,
			section = "screenshot"
	)
	default boolean notifyWhenTaken()
	{
		return true;
	}

}
