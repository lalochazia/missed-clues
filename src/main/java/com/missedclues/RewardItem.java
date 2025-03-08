package com.missedclues;

import java.util.Random;

public class RewardItem
{
    private static final Random RANDOM = new Random();
    private int itemId;
    private String itemName;
    private String quantity;
    private String rarity;
    private int weight;

    public RewardItem(int itemId, String itemName, String quantity, String rarity, int weight)
    {
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.rarity = rarity;
        this.weight = weight;
    }

    public int getItemId()
    {
        return itemId;
    }

    public String getItemName()
    {
        return itemName;
    }

    public String getQuantity()
    {
        return quantity;
    }

    public void setQuantity(String quantity)
    {
        this.quantity = quantity;
    }

    public String getRarity()
    {
        return rarity;
    }

    public int getWeight()
    {
        return weight;
    }

    public int getParsedQuantity()
    {
        if (quantity == null || quantity.trim().isEmpty())
        {
            return 1;
        }

        String normalized = quantity.replace("–", "-").trim();

        if (normalized.contains("-"))
        {
            String[] parts = normalized.split("-");
            if (parts.length == 2)
            {
                try
                {
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    if (max >= min)
                    {
                        return RANDOM.nextInt(max - min + 1) + min;
                    }
                }
                catch (NumberFormatException ignored)
                {
                }
            }
            return 1;
        }
        else
        {
            try
            {
                return Integer.parseInt(normalized);
            }
            catch (NumberFormatException e)
            {
                return 1;
            }
        }
    }
}