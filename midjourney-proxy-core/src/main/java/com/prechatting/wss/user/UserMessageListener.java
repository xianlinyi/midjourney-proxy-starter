package com.prechatting.wss.user;


import com.prechatting.ProxyProperties;
import com.prechatting.enums.MessageType;
import com.prechatting.support.ChannelPool;
import com.prechatting.support.DiscordChannel;
import com.prechatting.wss.handle.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.utils.data.DataObject;

import java.util.List;

@Slf4j
public class UserMessageListener  {
	private final ProxyProperties.DiscordConfig discordConfig;
	private final List<MessageHandler> messageHandlers ;

	private final ChannelPool channelPool;

	public UserMessageListener(ProxyProperties.DiscordConfig discordConfig, List<MessageHandler> messageHandlers, ChannelPool channelPool) {
		this.messageHandlers = messageHandlers;
		this.discordConfig = discordConfig;
		this.channelPool = channelPool;
	}

	public void onMessage(DataObject raw) {
		MessageType messageType = MessageType.of(raw.getString("t"));
		if (messageType == null || MessageType.DELETE == messageType) {
			return;
		}
		DataObject data = raw.getObject("d");
		DiscordChannel discordChannel = new DiscordChannel();
		discordChannel.setChannelId(data.getString("channel_id"));
		discordChannel.setGuildId(discordConfig.getGuildId());
		discordChannel.setUserToken(discordConfig.getUserToken());
		if (ignoreAndLogMessage(data, messageType)) {
			return;
		}
		for (MessageHandler messageHandler : this.messageHandlers) {
			messageHandler.handle(messageType, data, discordChannel);
		}
	}

	private boolean ignoreAndLogMessage(DataObject data, MessageType messageType) {
		String channelId = data.getString("channel_id");
		String id = discordConfig.getUserToken()+discordConfig.getGuildId()+channelId;
		if (!channelPool.containsChannel(id)) {
			return true;
		}
		String authorName = data.optObject("author").map(a -> a.getString("username")).orElse("System");
		log.debug("{} - {}: {} => userToken:{} guildId:{} channelID:{}", messageType.name(), authorName, data.opt("content").orElse(""),discordConfig.getUserToken(),discordConfig.getGuildId(),channelId);
		return false;
	}
}