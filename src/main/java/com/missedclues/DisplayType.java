package com.missedclues;

public enum DisplayType
{
    OVERLAY("Clue Pop-up"),
    CHAT_MESSAGE("Chat Message"),
    BOTH("Both"),
    NONE("None");

    private final String displayName;

    DisplayType(String displayName)
    {
        this.displayName = displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
