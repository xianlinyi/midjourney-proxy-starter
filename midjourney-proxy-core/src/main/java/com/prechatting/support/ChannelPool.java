package com.prechatting.support;

import com.prechatting.ProxyProperties;
import com.prechatting.enums.TaskAction;
import com.prechatting.service.DiscordConfigService;
import lombok.NoArgsConstructor;
import net.bytebuddy.implementation.bytecode.Throw;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor
public class ChannelPool {

    private DiscordConfigService discordConfigService;
    private List<ProxyProperties.DiscordConfig> discordConfigs;
    private final Map<String, DiscordChannel> channelMap = new ConcurrentHashMap<>();
    private final Map<String, Set<TaskAction>> channelStatusMap = new ConcurrentHashMap<>();

    public ChannelPool(DiscordConfigService discordConfigService, List<ProxyProperties.DiscordConfig> discordConfigs) {
        this.discordConfigService = discordConfigService;
        this.discordConfigs = discordConfigs;
    }

    public void addChannel(DiscordChannel channel) {
        // Add the channel to the map with its ID as the key
        channelMap.put(channel.getUserToken()+channel.getGuildId()+channel.getChannelId(), channel);
        // Initialize channel status as idle (true)
        channelStatusMap.put(channel.getUserToken()+channel.getGuildId()+channel.getChannelId(), new HashSet<>());
    }

    public synchronized DiscordChannel getFreeChannel(TaskAction taskAction) {
        int size = discordConfigs.size();
        for (int i = 0; i < size; i++) {
            ProxyProperties.DiscordConfig discordConfig = discordConfigService.getDiscordConfig(discordConfigs);
            for (Map.Entry<String, Set<TaskAction>> entry : channelStatusMap.entrySet()) {
                if (!entry.getValue().contains(TaskAction.IMAGINE) &&  entry.getKey().startsWith(discordConfig.getUserToken()+discordConfig.getGuildId())) {
                    String id = entry.getKey();
                    DiscordChannel channel = channelMap.get(id);
                    if (channel != null) {
                        setBusy(channel,taskAction);
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

    public boolean getStatus(String id,TaskAction taskAction) {
        Boolean status = !channelStatusMap.get(id).contains(taskAction);
        return status != null && status;
    }

    public void finish(DiscordChannel discordChannel,TaskAction taskAction) {
        String id = discordChannel.getUserToken()+discordChannel.getGuildId()+discordChannel.getChannelId();
        channelStatusMap.get(id).remove(taskAction);
    }

    public void setBusy(DiscordChannel discordChannel,TaskAction taskAction) {
        String id = discordChannel.getUserToken()+discordChannel.getGuildId()+discordChannel.getChannelId();
        channelStatusMap.get(id).add(taskAction);
    }

    public Integer getChannelCount() {
        return channelMap.size();
    }

}
