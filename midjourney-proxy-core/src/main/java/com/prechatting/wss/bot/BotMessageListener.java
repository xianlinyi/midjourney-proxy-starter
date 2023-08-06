package com.prechatting.wss.bot;

import cn.hutool.core.text.CharSequenceUtil;
import com.prechatting.ProxyProperties;
import com.prechatting.enums.MessageType;
import com.prechatting.support.ChannelPool;
import com.prechatting.support.DiscordChannel;
import com.prechatting.wss.handle.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;

import java.util.List;

@Slf4j
public class BotMessageListener extends ListenerAdapter implements ApplicationListener<ApplicationStartedEvent> {
	private final ProxyProperties.DiscordConfig discordConfig;
	private final List<MessageHandler> messageHandlers;
	private final ChannelPool channelPool;

	public BotMessageListener(ProxyProperties.DiscordConfig discordConfig,List<MessageHandler> messageHandlers,ChannelPool channelPool) {
		this.messageHandlers = messageHandlers;
		this.discordConfig = discordConfig;
		this.channelPool = channelPool;
	}

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		this.messageHandlers.addAll(event.getApplicationContext().getBeansOfType(MessageHandler.class).values());
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		Message message = event.getMessage();
		String channelId = message.getChannel().getId();
		DiscordChannel discordChannel = new DiscordChannel().setUserToken(discordConfig.getBotToken()).setGuildId(discordConfig.getGuildId()).setChannelId(channelId);
		if (ignoreAndLogMessage(message, MessageType.CREATE)) {
			return;
		}
		for (MessageHandler messageHandler : this.messageHandlers) {
			messageHandler.handle(MessageType.CREATE, message, discordChannel);
		}
	}

	@Override
	public void onMessageUpdate(MessageUpdateEvent event) {
		Message message = event.getMessage();
		String channelId = message.getChannel().getId();
		DiscordChannel discordChannel = new DiscordChannel().setUserToken(discordConfig.getBotToken()).setGuildId(discordConfig.getGuildId()).setChannelId(channelId);
		if (ignoreAndLogMessage(message, MessageType.UPDATE)) {
			return;
		}
		for (MessageHandler messageHandler : this.messageHandlers) {
			messageHandler.handle(MessageType.UPDATE, message, discordChannel);
		}
	}

	private boolean ignoreAndLogMessage(Message message, MessageType messageType) {
		String channelId = message.getChannel().getId();
		String id = discordConfig.getBotToken()+discordConfig.getGuildId()+channelId;
		if (!channelPool.containsChannel(id)) {
			return true;
		}
		String authorName = message.getAuthor().getName();
		if (CharSequenceUtil.isBlank(authorName)) {
			authorName = "System";
		}
		log.debug("{} - {}: {}", messageType.name(), authorName, message.getContentRaw());
		return false;
	}

}
