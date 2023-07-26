package com.prechatting.service;

import com.prechatting.ProxyProperties;

import java.util.List;

public class DefaultDiscordConfigService implements DiscordConfigService {
    private int currentIndex = 0;

    @Override
    public ProxyProperties.DiscordConfig getDiscordConfig(List<ProxyProperties.DiscordConfig> discordConfigs) {
        // Check if the list is not empty
        if (discordConfigs.isEmpty()) {
            return null; // Return null if there are no Discord configurations
        }
        // Get the DiscordConfig at the current index
        ProxyProperties.DiscordConfig selectedConfig = discordConfigs.get(currentIndex);
        // Increment the index for the next round-robin selection
        currentIndex = (currentIndex + 1) % discordConfigs.size();

        return selectedConfig;
    }
}
