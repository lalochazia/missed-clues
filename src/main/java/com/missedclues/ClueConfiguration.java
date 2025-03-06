package com.missedclues;

public class ClueConfiguration
{
    private String chatTrigger;
    private String jsonResource;
    private int minItems;
    private int maxItems;
    private String tier;

    public String getChatTrigger()
    {
        return chatTrigger;
    }

    public String getJsonResource()
    {
        return jsonResource;
    }

    public int getMinItems()
    {
        return minItems;
    }

    public int getMaxItems()
    {
        return maxItems;
    }

    public String getTier()
    {
        return tier;
    }

}