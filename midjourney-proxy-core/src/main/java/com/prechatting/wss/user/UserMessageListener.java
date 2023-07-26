package com.prechatting.wss.user;


import com.prechatting.ProxyProperties;
import com.prechatting.enums.MessageType;
import com.prechatting.wss.handle.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.utils.data.DataObject;

import java.util.List;

@Slf4j
public class UserMessageListener  {
	private final ProxyProperties.DiscordConfig discordConfig;
	private final List<MessageHandler> messageHandlers ;

	public UserMessageListener(ProxyProperties.DiscordConfig discordConfig, List<MessageHandler> messageHandlers) {
		this.messageHandlers = messageHandlers;
		this.discordConfig = discordConfig;
	}

	public void onMessage(DataObject raw) {
		MessageType messageType = MessageType.of(raw.getString("t"));
		if (messageType == null || MessageType.DELETE == messageType) {
			return;
		}
		DataObject data = raw.getObject("d");
		if (ignoreAndLogMessage(data, messageType)) {
			return;
		}
		for (MessageHandler messageHandler : this.messageHandlers) {
			messageHandler.handle(messageType, data);
		}
	}

	private boolean ignoreAndLogMessage(DataObject data, MessageType messageType) {
		String channelId = data.getString("channel_id");
		if (!discordConfig.getChannelId().equals(channelId)) {
			return true;
		}
		String authorName = data.optObject("author").map(a -> a.getString("username")).orElse("System");
		log.debug("{} - {}: {} => userToken:{} guildId:{} channelID:{}", messageType.name(), authorName, data.opt("content").orElse(""),discordConfig.getUserToken(),discordConfig.getGuildId(),discordConfig.getChannelId());
		return false;
	}
}