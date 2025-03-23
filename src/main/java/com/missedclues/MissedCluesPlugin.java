package com.missedclues;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatLineBuffer;
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
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.ImageCapture;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.ItemContainer;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.client.callback.ClientThread;
import javax.inject.Inject;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

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
	private ChatMessageManager chatMessageManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MissedCluesConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MissedCluesOverlay missedCluesOverlay;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private Gson gson;

	@Inject
	private ConfigManager configManager;

	@Inject
	private DrawManager drawManager;

	@Inject
	private ImageCapture imageCapture;

	@Inject
	private ScheduledExecutorService executor;

	private final Random random = new Random(System.currentTimeMillis());
	private Item[] previousInventory = null;
	private static final Pattern MISSED_CLUES_PATTERN = Pattern.compile("^!missed (?<tier>beginner|easy|medium|hard|elite|master)$", Pattern.CASE_INSENSITIVE);
	private List<ClueConfiguration> clueConfigs = new ArrayList<>();
	private final Map<String, List<RewardItem>> rewardTables = new HashMap<>();
	private static final int COIN_DIFFERENCE_LOW = 8;
	private static final int COIN_DIFFERENCE_HIGH = 12;

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
		migrateConfig();
		loadAllRewardTables();
		overlayManager.add(missedCluesOverlay);
		mouseManager.registerMouseListener(mouseListener);
		client.getCanvas().addKeyListener(escKeyListener);
	}

	@Override
	protected void shutDown()
	{
		log.info("Missed Clues plugin stopped!");
		overlayManager.remove(missedCluesOverlay);
		mouseManager.unregisterMouseListener(mouseListener);
		client.getCanvas().removeKeyListener(escKeyListener);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event) {
		if (event.getGroupId() == InterfaceID.DIALOG_SPRITE) {
			clientThread.invokeLater(() -> {
				Widget textChild = client.getWidget(InterfaceID.DIALOG_SPRITE, 2);
				if (textChild != null) {
					String text = textChild.getText();
					String cleanText = text.replace("<br>", " ");
					if (cleanText.equals("Watson hands you a master clue scroll.")) {
						client.addChatMessage(
								ChatMessageType.GAMEMESSAGE,
								"runelite",
								"You have a funny feeling Watson has done your clues...",
								null
						);
						rollAllTiers();
					}
				}
			});
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event) {
		if (event.getContainerId() != InventoryID.INVENTORY.getId()) {
			return;
		}

		ItemContainer container = event.getItemContainer();
		if (container == null) {
			return;
		}

		Item[] currentInventory = container.getItems();

		if (previousInventory != null) {
			boolean itemRemoved = false;
			boolean coinsAdded = false;
			String tier = "";

			for (Item prevItem : previousInventory) {
				if (prevItem != null && !containsItem(currentInventory, prevItem.getId())) {
					ItemComposition itemComp = itemManager.getItemComposition(prevItem.getId());
					String removedItemName = itemComp.getName().toLowerCase();
					if (removedItemName.startsWith("clue scroll (") || removedItemName.startsWith("challenge scroll (")) {
						tier = removedItemName.contains("clue scroll") ?
								removedItemName.replace("clue scroll (", "").replace(")", "") :
								removedItemName.replace("challenge scroll (", "").replace(")", "");
						itemRemoved = true;
					}
				}
			}

			int previousCoins = getItemQuantity(previousInventory, ItemID.COINS_995);
			int currentCoins = getItemQuantity(currentInventory, ItemID.COINS_995);
			int coinDifference = currentCoins - previousCoins;
			if (coinDifference == COIN_DIFFERENCE_LOW || coinDifference == COIN_DIFFERENCE_HIGH) {
				coinsAdded = true;
			}

			if (itemRemoved && coinsAdded && !tier.isEmpty()) {
				chatMessageManager.queue(QueuedMessage.builder()
						.type(ChatMessageType.GAMEMESSAGE)
						.runeLiteFormattedMessage("You incinerate your " + tier + " clue scroll.")
						.build());
			}
		}
		previousInventory = currentInventory.clone();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() == ChatMessageType.PUBLICCHAT
				|| event.getType() == ChatMessageType.PRIVATECHAT
				|| event.getType() == ChatMessageType.FRIENDSCHAT
				|| event.getType() == ChatMessageType.CLAN_CHAT) {

			String message = event.getMessage();
			Matcher matcher = MISSED_CLUES_PATTERN.matcher(message);

			if (message.equalsIgnoreCase("!lastmissed")) {
				long lastValue = config.lastMissedValue();
				String lastTier = config.lastMissedTier();

				if (lastValue > 0 && !lastTier.isEmpty()) {
					client.addChatMessage(
							ChatMessageType.GAMEMESSAGE,
							"",
							String.format("Last missed clue: %,dgp (%s)", lastValue, lastTier),
							null
					);
				} else {
					client.addChatMessage(
							ChatMessageType.GAMEMESSAGE,
							"",
							"No missed clue recorded yet.",
							null
					);
				}
				return;
			}

			if (matcher.find()) {
				String tier = matcher.group("tier").toLowerCase();
				int missedCount = getMissedCountFromConfig(tier);

				if (missedCount >= 0) {
					String displayTier = tier.substring(0, 1).toUpperCase() + tier.substring(1);
					client.addChatMessage(
							ChatMessageType.GAMEMESSAGE,
							"",
							String.format("%s clue missed count: %d", displayTier, missedCount),
							null
					);
				}
				return;
			}
		}

		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = event.getMessage();

		ClueConfiguration clueConfig = clueConfigs.stream()
				.filter(cfg -> message.equals(cfg.getChatTrigger()))
				.findFirst()
				.orElse(null);

		if (clueConfig == null)
		{
			return;
		}

		DisplayType displayType = getDisplayTypeForTier(clueConfig.getTier());

		int currentCount;
		switch (clueConfig.getTier().toLowerCase())
		{
			case "beginner":
				currentCount = config.missedBeginnerCount() + 1;
				config.missedBeginnerCount(currentCount);
				break;

			case "easy":
				currentCount = config.missedEasyCount() + 1;
				config.missedEasyCount(currentCount);
				break;

			case "medium":
				currentCount = config.missedMediumCount() + 1;
				config.missedMediumCount(currentCount);
				break;

			case "hard":
				currentCount = config.missedHardCount() + 1;
				config.missedHardCount(currentCount);
				break;

			case "elite":
				currentCount = config.missedEliteCount() + 1;
				config.missedEliteCount(currentCount);
				break;

			case "master":
				currentCount = config.missedMasterCount() + 1;
				config.missedMasterCount(currentCount);
				break;

			default:
				return;
		}

		if (displayType == DisplayType.NONE)
		{
			return;
		}

		List<RewardItem> rewardList = rewardTables.get(clueConfig.getChatTrigger());
		if (rewardList == null || rewardList.isEmpty())
		{
			if (displayType == DisplayType.BOTH || displayType == DisplayType.CHAT_MESSAGE)
			{
				client.addChatMessage(
						ChatMessageType.GAMEMESSAGE,
						"",
						"No rewards found for " + clueConfig.getChatTrigger(),
						null
				);
			}
			return;
		}

		int minItems = clueConfig.getMinItems();
		int maxItems = clueConfig.getMaxItems();
		int countToPick = random.nextInt(maxItems - minItems + 1) + minItems;

		List<RewardItem> chosenItems = pickWeightedItems(rewardList, countToPick);
		chosenItems = consolidateItems(chosenItems);

		if (chosenItems.isEmpty())
		{
			if (displayType == DisplayType.BOTH || displayType == DisplayType.CHAT_MESSAGE)
			{
				client.addChatMessage(
						ChatMessageType.GAMEMESSAGE,
						"",
						"Could not select any weighted items.",
						null
				);
			}
			return;
		}

		if (displayType == DisplayType.CHAT_MESSAGE || displayType == DisplayType.BOTH)
		{
			String itemsList = chosenItems.stream()
					.map(item -> item.getQuantity() + "x " + item.getItemName())
					.collect(Collectors.joining(", "));

			client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"runelite",
					"You have a funny feeling you would have received: " + itemsList,
					null
			);

			long totalPrice = 0;
			for (RewardItem item : chosenItems)
			{
				int gePriceEach = itemManager.getItemPrice(item.getItemId());
				totalPrice += (long) gePriceEach * item.getParsedQuantity();
			}

			config.lastMissedValue(totalPrice);
			config.lastMissedTier(clueConfig.getTier());

			if (config.valuableThreshold() > 0 && totalPrice > config.valuableThreshold())
			{
				String fileName = String.format("%s clue (%,dgp)",
						clueConfig.getTier().substring(0, 1).toUpperCase() + clueConfig.getTier().substring(1).toLowerCase(),
						totalPrice
				);
				takeScreenshot(fileName);
				log.debug("Taking screenshot of valuable clue reward worth {} gp", totalPrice);
			}


			String formattedPrice = String.format("%,d", totalPrice);
			client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"",
					"Your loot would have been worth " + formattedPrice + " coins!",
					null
			);
			client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"",
					"Your missed " + clueConfig.getTier() + " clue count is: <col=ff0000>" + currentCount + "</col>",
					null
			);
		}

		if (displayType == DisplayType.OVERLAY || displayType == DisplayType.BOTH)
		{
			showItemsInOverlay(chosenItems);
		}
	}

	private DisplayType getDisplayTypeForTier(String tier)
	{
		if (tier == null)
		{
			return DisplayType.NONE;
		}

		switch (tier.toLowerCase())
		{
			case "beginner": return config.beginnerDisplay();
			case "easy":     return config.easyDisplay();
			case "medium":   return config.mediumDisplay();
			case "hard":     return config.hardDisplay();
			case "elite":    return config.eliteDisplay();
			case "master":   return config.masterDisplay();
			default:         return DisplayType.NONE;
		}
	}

	private void rollAllTiers() {
		DisplayType displayType = config.watsonDisplay();
		if (displayType == DisplayType.NONE) {
			return;
		}

		Map<String, List<ItemStack>> allTierStacks = new LinkedHashMap<>();
		long totalValue = 0;

		String[] allTiers = {"easy", "medium", "hard", "elite"};

		if (displayType == DisplayType.CHAT_MESSAGE || displayType == DisplayType.BOTH) {
			client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"runelite",
					"You have a funny feeling like you would have received:",
					null
			);
		}

		for (String tier : allTiers) {
			ClueConfiguration clueConfig = clueConfigs.stream()
					.filter(cfg -> cfg.getTier().equalsIgnoreCase(tier))
					.findFirst()
					.orElse(null);

			if (clueConfig != null) {
				List<RewardItem> rewardList = rewardTables.get(clueConfig.getChatTrigger());
				if (rewardList != null && !rewardList.isEmpty()) {
					int minItems = clueConfig.getMinItems();
					int maxItems = clueConfig.getMaxItems();
					int countToPick = random.nextInt(maxItems - minItems + 1) + minItems;

					List<RewardItem> chosenItems = pickWeightedItems(rewardList, countToPick);
					chosenItems = consolidateItems(chosenItems);

					if (!chosenItems.isEmpty()) {
						List<ItemStack> tierStacks = chosenItems.stream()
								.map(item -> new ItemStack(item.getItemId(), item.getParsedQuantity()))
								.collect(Collectors.toList());

						allTierStacks.put(tier, tierStacks);

						if (displayType == DisplayType.CHAT_MESSAGE || displayType == DisplayType.BOTH) {
							String itemsList = chosenItems.stream()
									.map(item -> item.getQuantity() + "x " + item.getItemName())
									.collect(Collectors.joining(", "));

							long tierTotal = 0;
							for (RewardItem item : chosenItems) {
								int gePriceEach = itemManager.getItemPrice(item.getItemId());
								tierTotal += (long) gePriceEach * item.getParsedQuantity();
							}
							totalValue += tierTotal;

							client.addChatMessage(
									ChatMessageType.GAMEMESSAGE,
									"",
									"[" + tier.substring(0, 1).toUpperCase() + tier.substring(1) + "] " + itemsList,
									null
							);
						}
					}
				}
			}
		}

		if (totalValue > 0 && (displayType == DisplayType.CHAT_MESSAGE || displayType == DisplayType.BOTH)) {
			String formattedTotalPrice = String.format("%,d", totalValue);
			client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"",
					"Your loot would have been worth: " + formattedTotalPrice + " coins!",
					null
			);
		}

		if (displayType == DisplayType.OVERLAY || displayType == DisplayType.BOTH) {
			missedCluesOverlay.setAllTierStacks(allTierStacks);
		}
	}

	private void showItemsInOverlay(List<RewardItem> chosenItems)
	{
		List<ItemStack> stacks = chosenItems.stream()
				.map(item -> new ItemStack(item.getItemId(), item.getParsedQuantity()))
				.collect(Collectors.toList());

		missedCluesOverlay.displayItems(false);
		missedCluesOverlay.setItemStacks(stacks);
		missedCluesOverlay.displayItems(true);
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

	private void loadClueConfigs()
	{
		try (InputStream is = getClass().getResourceAsStream("/clue_config.json"))
		{
			if (is == null)
			{
				log.warn("Failed to find clue_config.json in resources.");
				return;
			}

			clueConfigs = gson.fromJson(
					new InputStreamReader(is, StandardCharsets.UTF_8),
					new TypeToken<List<ClueConfiguration>>() {}.getType()
			);
			log.debug("Loaded {} ClueConfiguration entries.", clueConfigs.size());
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

				List<RewardItem> items = gson.fromJson(
						new InputStreamReader(is, StandardCharsets.UTF_8),
						new TypeToken<List<RewardItem>>() {}.getType()
				);

				rewardTables.put(cfg.getChatTrigger(), items);
				log.debug("Loaded {} items for trigger \"{}\" from {}",
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

	private void migrateConfig() {
		String migrated = configManager.getConfiguration("Clue Tiers", "migrated");
		if ("1".equals(migrated)) {
			return;
		}

		String[][] toggleMappings = {
				{"beginnerToggle", "beginnerDisplay"},
				{"easyToggle", "easyDisplay"},
				{"mediumToggle", "mediumDisplay"},
				{"hardToggle", "hardDisplay"},
				{"eliteToggle", "eliteDisplay"},
				{"masterToggle", "masterDisplay"}
		};

		for (String[] mapping : toggleMappings) {
			String oldKey = mapping[0];
			String newKey = mapping[1];

			Boolean oldValue = configManager.getConfiguration("Clue Tiers", oldKey, Boolean.class);
			if (oldValue != null) {
				DisplayType newValue = oldValue ? DisplayType.BOTH : DisplayType.NONE;
				configManager.setConfiguration("Clue Tiers", newKey, newValue);
			}
		}

		configManager.setConfiguration("Clue Tiers", "migrated", 1);
	}

	private int getMissedCountFromConfig(String tier) {
		switch (tier) {
			case "beginner":
				return config.missedBeginnerCount();
			case "easy":
				return config.missedEasyCount();
			case "medium":
				return config.missedMediumCount();
			case "hard":
				return config.missedHardCount();
			case "elite":
				return config.missedEliteCount();
			case "master":
				return config.missedMasterCount();
			default:
				return -1;
		}
	}

	private void takeScreenshot(String fileName)
	{
		Consumer<Image> imageCallback = (img) ->
		{
			executor.submit(() -> {
				try
				{
					takeScreenshot((BufferedImage) img, fileName);
				}
				catch (Exception ex)
				{
					log.warn("Error taking screenshot", ex);
				}
			});
		};
		drawManager.requestNextFrameListener(imageCallback);
	}

	private void takeScreenshot(BufferedImage image, String fileName)
	{
		imageCapture.saveScreenshot(image, fileName, "Missed Clues", config.notifyWhenTaken(), false);

	}

	private final KeyAdapter escKeyListener = new KeyAdapter()
	{
		@Override
		public void keyPressed(KeyEvent e)
		{
			if ((missedCluesOverlay.isDisplayingItems() || missedCluesOverlay.isDisplayingAllTiers()) && e.getKeyCode() == KeyEvent.VK_ESCAPE)
			{
				missedCluesOverlay.displayItems(false);
				missedCluesOverlay.displayAllTiers(false);
			}
		}
	};

	private final MouseAdapter mouseListener = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent event)
		{
			if ((missedCluesOverlay.isDisplayingItems() || missedCluesOverlay.isDisplayingAllTiers()) && missedCluesOverlay.getCloseButtonBounds() != null)
			{
				if (missedCluesOverlay.getCloseButtonBounds().contains(event.getPoint()))
				{
					missedCluesOverlay.displayItems(false);
					missedCluesOverlay.displayAllTiers(false);
					event.consume();
				}
			}
			return event;
		}
	};

	private boolean containsItem(Item[] items, int itemId) {
		for (Item item : items) {
			if (item != null && item.getId() == itemId) {
				return true;
			}
		}
		return false;
	}
	private int getItemQuantity(Item[] items, int itemId) {
		for (Item item : items) {
			if (item != null && item.getId() == itemId) {
				return item.getQuantity();
			}
		}
		return 0;
	}
}
