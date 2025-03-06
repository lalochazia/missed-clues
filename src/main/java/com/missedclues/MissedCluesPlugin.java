package com.missedclues;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.inject.Inject;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
		name = "Missed Clues"
)
public class MissedCluesPlugin extends Plugin
{
	@Inject
	private ItemManager itemManager;

	@Inject
	private Client client;

	@Inject
	private MissedCluesConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Overlay overlay;

	@Inject
	private MouseManager mouseManager;

	private final Random random = new Random();

	private final KeyAdapter escKeyListener = new KeyAdapter()
	{
		@Override
		public void keyPressed(KeyEvent e)
		{
			if (overlay.isDisplayingItems() && e.getKeyCode() == KeyEvent.VK_ESCAPE)
			{
				overlay.displayItems(false);
			}
		}
	};

	private List<ClueConfiguration> clueConfigs = new ArrayList<>();

	private final Map<String, List<RewardItem>> rewardTables = new HashMap<>();

	private final MouseAdapter mouseListener = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent event)
		{
			if (overlay.isDisplayingItems() && overlay.getCloseButtonBounds() != null)
			{
				if (overlay.getCloseButtonBounds().contains(event.getPoint()))
				{
					overlay.displayItems(false);
					event.consume();
				}
			}
			return event;
		}
	};

	@Provides
	MissedCluesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MissedCluesConfig.class);
	}

	@Override
	protected void startUp()
	{
		log.info("Missed Clues plugin started!");

		loadClueConfigs();
		loadAllRewardTables();

		overlayManager.add(overlay);

		// Register our mouse listener
		mouseManager.registerMouseListener(mouseListener);
		client.getCanvas().addKeyListener(escKeyListener);
	}

	@Override
	protected void shutDown()
	{
		log.info("Missed Clues plugin stopped!");
		overlayManager.remove(overlay);
		mouseManager.unregisterMouseListener(mouseListener);
		client.getCanvas().removeKeyListener(escKeyListener);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		String message = event.getMessage();
		ClueConfiguration clueConfig = clueConfigs.stream()
				.filter(cfg -> message.equals(cfg.getChatTrigger()))
				.findFirst()
				.orElse(null);

		if (clueConfig == null)
		{
			return;
		}

		if (!isTierEnabled(clueConfig.getTier()))
		{
			return;
		}

		List<RewardItem> rewardList = rewardTables.get(clueConfig.getChatTrigger());
		if (rewardList == null || rewardList.isEmpty())
		{
			client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"",
					"No rewards found for " + clueConfig.getChatTrigger(),
					null
			);
			return;
		}

		int minItems = clueConfig.getMinItems();
		int maxItems = clueConfig.getMaxItems();
		int countToPick = random.nextInt(maxItems - minItems + 1) + minItems;

		List<RewardItem> chosenItems = pickWeightedItems(rewardList, countToPick);
		chosenItems = consolidateItems(chosenItems);

		if (chosenItems.isEmpty())
		{
			client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"",
					"Could not select any weighted items.",
					null
			);
			return;
		}

		client.addChatMessage(
				ChatMessageType.GAMEMESSAGE,
				"",
				"You have a funny feeling you would have received:",
				null
		);

		for (RewardItem item : chosenItems)
		{
			client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"",
					item.getQuantity() + "x " + item.getItemName(),
					null
			);
		}

		showItemsInOverlay(chosenItems);
	}

	private boolean isTierEnabled(String tier)
	{
		if (tier == null)
		{
			return false;
		}

		switch (tier.toLowerCase())
		{
			case "beginner":
				return config.beginnerToggle();
			case "easy":
				return config.easyToggle();
			case "medium":
				return config.mediumToggle();
			case "hard":
				return config.hardToggle();
			case "elite":
				return config.eliteToggle();
			case "master":
				return config.masterToggle();
			default:
				return true;
		}
	}

	private void loadClueConfigs()
	{
		try (InputStream is = getClass().getResourceAsStream("/clue_config.json"))
		{
			if (is == null)
			{
				log.warn("Failed to find clue_config.json in resources.");
				return;
			}

			Gson gson = new Gson();
			clueConfigs = gson.fromJson(
					new InputStreamReader(is, StandardCharsets.UTF_8),
					new TypeToken<List<ClueConfiguration>>() {}.getType()
			);
			log.info("Loaded {} ClueConfiguration entries.", clueConfigs.size());
		}
		catch (Exception e)
		{
			log.error("Error reading /clue_config.json", e);
		}
	}

	private void loadAllRewardTables()
	{
		for (ClueConfiguration cfg : clueConfigs)
		{
			try (InputStream is = getClass().getResourceAsStream(cfg.getJsonResource()))
			{
				if (is == null)
				{
					log.warn("Failed to locate {}", cfg.getJsonResource());
					continue;
				}
				Gson gson = new Gson();
				List<RewardItem> items = gson.fromJson(
						new InputStreamReader(is, StandardCharsets.UTF_8),
						new TypeToken<List<RewardItem>>() {}.getType()
				);

				rewardTables.put(cfg.getChatTrigger(), items);
				log.info("Loaded {} items for trigger \"{}\" from {}",
						items.size(),
						cfg.getChatTrigger(),
						cfg.getJsonResource()
				);
			}
			catch (Exception e)
			{
				log.error("Error reading {}", cfg.getJsonResource(), e);
			}
		}
	}

	private RewardItem getWeightedRandomReward(List<RewardItem> items)
	{
		int totalWeight = items.stream().mapToInt(RewardItem::getWeight).sum();
		if (totalWeight <= 0)
		{
			return null;
		}
		int roll = random.nextInt(totalWeight) + 1;
		int runningSum = 0;

		for (RewardItem candidate : items)
		{
			runningSum += candidate.getWeight();
			if (runningSum >= roll)
			{
				return candidate;
			}
		}
		return null;
	}

	private List<RewardItem> pickWeightedItems(List<RewardItem> sourceList, int countToPick)
	{
		List<RewardItem> chosenItems = new ArrayList<>();
		for (int i = 0; i < countToPick; i++)
		{
			RewardItem reward = getWeightedRandomReward(sourceList);
			if (reward != null)
			{
				int parsedQty = reward.getParsedQuantity();
				reward.setQuantity(String.valueOf(parsedQty));
				chosenItems.add(reward);
			}
		}
		return chosenItems;
	}

	private List<RewardItem> consolidateItems(List<RewardItem> items)
	{
		Map<Integer, RewardItem> byItemId = new HashMap<>();
		for (RewardItem item : items)
		{
			int itemId = item.getItemId();
			int parsedQty = item.getParsedQuantity();

			if (!byItemId.containsKey(itemId))
			{
				byItemId.put(itemId, new RewardItem(
						itemId,
						item.getItemName(),
						String.valueOf(parsedQty),
						item.getRarity(),
						item.getWeight()
				));
			}
			else
			{
				RewardItem existing = byItemId.get(itemId);
				int existingQty = existing.getParsedQuantity();
				existing.setQuantity(String.valueOf(existingQty + parsedQty));
			}
		}
		return new ArrayList<>(byItemId.values());
	}

	private void showItemsInOverlay(List<RewardItem> chosenItems)
	{
		long totalPrice = 0;
		for (RewardItem item : chosenItems)
		{
			int gePriceEach = itemManager.getItemPrice(item.getItemId());
			totalPrice += (long) gePriceEach * item.getParsedQuantity();
		}

		String formattedPrice = String.format("%,d", totalPrice);
		client.addChatMessage(
				ChatMessageType.GAMEMESSAGE,
				"",
				"Your loot would have been worth " + formattedPrice + " coins!",
				null
		);

		List<ItemStack> stacks = chosenItems.stream()
				.map(item -> new ItemStack(item.getItemId(), item.getParsedQuantity()))
				.collect(Collectors.toList());

		overlay.displayItems(false);
		overlay.setItemStacks(stacks);
		overlay.displayItems(true);

	}
}