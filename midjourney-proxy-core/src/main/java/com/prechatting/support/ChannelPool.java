package com.prechatting.support;

import com.prechatting.ProxyProperties;
import com.prechatting.service.DiscordConfigService;
import lombok.NoArgsConstructor;
import net.bytebuddy.implementation.bytecode.Throw;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor
public class ChannelPool {

    private DiscordConfigService discordConfigService;
    private List<ProxyProperties.DiscordConfig> discordConfigs;
    private final Map<String, DiscordChannel> channelMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> channelStatusMap = new ConcurrentHashMap<>();

    public ChannelPool(DiscordConfigService discordConfigService, List<ProxyProperties.DiscordConfig> discordConfigs) {
        this.discordConfigService = discordConfigService;
        this.discordConfigs = discordConfigs;
    }

    public void addChannel(DiscordChannel channel) {
        // Add the channel to the map with its ID as the key
        channelMap.put(channel.getUserToken()+channel.getGuildId()+channel.getChannelId(), channel);
        // Initialize channel status as idle (true)
        channelStatusMap.put(channel.getUserToken()+channel.getGuildId()+channel.getChannelId(), true);
    }

    public synchronized DiscordChannel getFreeChannel() {
        int size = discordConfigs.size();
        for (int i = 0; i < size; i++) {
            ProxyProperties.DiscordConfig discordConfig = discordConfigService.getDiscordConfig(discordConfigs);
            for (Map.Entry<String, Boolean> entry : channelStatusMap.entrySet()) {
                if (entry.getValue() &&  entry.getKey().startsWith(discordConfig.getUserToken()+discordConfig.getGuildId())) {
                    String id = entry.getKey();
                    DiscordChannel channel = channelMap.get(id);
                    if (channel != null) {
                        setBusy(channel);
                        return channel;
                    }
                }
            }
        }
        throw new RuntimeException("No free channel");
    }

    public boolean containsChannel(String id) {
        return channelMap.containsKey(id);
    }

    public boolean getStatus(String id) {
        Boolean status = channelStatusMap.get(id);
        return status != null && status;
    }

    public void finish(DiscordChannel discordChannel) {
        String id = discordChannel.getUserToken()+discordChannel.getGuildId()+discordChannel.getChannelId();
        channelStatusMap.put(id, true);
    }

    public void setBusy(DiscordChannel discordChannel) {
        String id = discordChannel.getUserToken()+discordChannel.getGuildId()+discordChannel.getChannelId();
        channelStatusMap.put(id, false);
    }

    public Integer getChannelCount() {
        return channelMap.size();
    }

}
