package com.missedclues;

import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MissedCluesOverlay extends net.runelite.client.ui.overlay.Overlay {
    private final Client client;
    private final ItemManager itemManager;
    private final BufferedImage closeButtonImage;
    private final BufferedImage closeButtonHoveredImage;
    private final BufferedImage incineratorImage;
    private final BufferedImage watsonImage;
    private Rectangle closeButtonBounds;
    private boolean displayItems;
    private List<ItemStack> itemStacks = new ArrayList<>();
    private Map<String, List<ItemStack>> allTierStacks = new LinkedHashMap<>();
    private boolean displayAllTiers = false;

    private static final Set<Integer> NOTED_IDS = Set.of(
            12913, 269, 391, 245, 225, 207, 3049, 3051, 451, 2363,
            1747, 1391, 11951, 3024, 6685, 2444, 7060, 7218, 8778,
            8780, 8782, 3016, 2452, 2436, 379, 385, 373, 333,
            329, 1965, 315, 325, 347
    );

    private static final Set<Integer> QUANTITY_SENSITIVE_IDS = Set.of(
            882, 884, 995, 4561, 9194, 9245, 10476, 5289, 5315, 5316, 617
    );

    @Inject
    public MissedCluesOverlay(Client client, ItemManager itemManager) {
        this.client = client;
        this.itemManager = itemManager;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(200.0f);

        incineratorImage = ImageUtil.loadImageResource(getClass(), "/incinerator.png");
        watsonImage = ImageUtil.loadImageResource(getClass(), "/watson.png");
        closeButtonImage = ImageUtil.loadImageResource(getClass(), "/closeButton.png");
        closeButtonHoveredImage = ImageUtil.loadImageResource(getClass(), "/closeButtonHovered.png");
    }

    public void displayItems(boolean show) {
        this.displayItems = show;
    }

    public boolean isDisplayingItems() {
        return this.displayItems;
    }

    public void displayAllTiers(boolean show) {
        this.displayAllTiers = show;
    }

    public boolean isDisplayingAllTiers() {
        return this.displayAllTiers;
    }

    public void setItemStacks(List<ItemStack> stacks) {
        this.itemStacks = stacks;
    }

    public void setAllTierStacks(Map<String, List<ItemStack>> stacks) {
        this.allTierStacks = stacks;
        this.displayAllTiers = true;
        this.displayItems = false;
    }

    public Rectangle getCloseButtonBounds() {
        return closeButtonBounds;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if ((!displayItems && !displayAllTiers) || (displayItems && itemStacks.isEmpty()) || (displayAllTiers && allTierStacks.isEmpty())) {
            return null;
        }

        final int canvasWidth = client.getCanvasWidth();
        final int canvasHeight = client.getCanvasHeight();

        final int startX;
        final int startY;

        if (canvasWidth <= 1000 && canvasHeight <= 650) {
            startX = (canvasWidth - 309) / 2;
            startY = (canvasHeight - 296) / 2;
        } else {
            startX = canvasWidth / 2 - 24;
            startY = canvasHeight / 3 - 24;
        }

        BufferedImage backgroundImage = displayAllTiers ? watsonImage : incineratorImage;
        if (backgroundImage != null) {
            int incX = startX - 140;
            int incY = startY - 70;
            graphics.drawImage(backgroundImage, incX, incY, null);

            if (closeButtonImage != null) {
                int closeX = incX + backgroundImage.getWidth() - closeButtonImage.getWidth() + 40;
                int closeY = incY + 15;

                closeButtonBounds = new Rectangle(
                        closeX,
                        closeY,
                        closeButtonImage.getWidth(),
                        closeButtonImage.getHeight()
                );

                net.runelite.api.Point netMousePos = client.getMouseCanvasPosition();
                Point mousePos = new Point(netMousePos.getX(), netMousePos.getY());

                boolean isHovered = closeButtonBounds.contains(mousePos);
                BufferedImage toDraw = isHovered ? closeButtonHoveredImage : closeButtonImage;
                graphics.drawImage(toDraw, closeX, closeY, null);
            }
        }

        final int itemsPerRow = 3;
        int x = startX;
        int y = startY + (displayAllTiers ? -30 : 0);

        if (displayAllTiers) {
            List<ItemStack> allItems = new ArrayList<>();
            for (List<ItemStack> tierItems : allTierStacks.values()) {
                allItems.addAll(tierItems);
            }

            renderItems(graphics, allItems, x, y, itemsPerRow);
        } else {
            renderItems(graphics, itemStacks, x, y, itemsPerRow);
        }

        return null;
    }


    private void renderItems(Graphics2D graphics, List<ItemStack> items, int x, int y, int itemsPerRow) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            int itemId = stack.getItemId();
            int quantity = stack.getQuantity();

            if (NOTED_IDS.contains(itemId)) {
                itemId += 1;
            }

            BufferedImage itemImage;
            if (QUANTITY_SENSITIVE_IDS.contains(stack.getItemId())) {
                itemImage = itemManager.getImage(itemId, quantity, true);
            } else {
                itemImage = itemManager.getImage(itemId);
            }

            if (itemImage != null) {
                graphics.drawImage(itemImage, x, y, null);

                if (quantity > 1 && !QUANTITY_SENSITIVE_IDS.contains(stack.getItemId())) {
                    String qtyText = String.valueOf(quantity);
                    FontMetrics fm = graphics.getFontMetrics();
                    int textX = x;
                    int textY = y + fm.getAscent();
                    graphics.setColor(Color.BLACK);
                    graphics.drawString(qtyText, textX + 1, textY + 1);
                    graphics.setColor(Color.YELLOW);
                    graphics.drawString(qtyText, textX, textY);
                }

                if ((i + 1) % itemsPerRow == 0) {
                    x = x - (itemImage.getWidth()) * (itemsPerRow - 1);
                    y += itemImage.getHeight();
                } else {
                    x += itemImage.getWidth();
                }
            }
        }
    }
}