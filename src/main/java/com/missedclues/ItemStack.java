package com.missedclues;

public class ItemStack
{
    private final int itemId;
    private final int quantity;

    public ItemStack(int itemId, int quantity)
    {
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public int getItemId()
    {
        return itemId;
    }

    public int getQuantity()
    {
        return quantity;
    }
}