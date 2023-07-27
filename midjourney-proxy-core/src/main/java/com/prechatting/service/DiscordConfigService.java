package com.prechatting.service;

import com.prechatting.ProxyProperties;
import com.prechatting.support.DiscordChannel;

import java.util.List;

public interface DiscordConfigService {
     ProxyProperties.DiscordConfig getDiscordConfig(List<ProxyProperties.DiscordConfig> discordConfigs);

}
