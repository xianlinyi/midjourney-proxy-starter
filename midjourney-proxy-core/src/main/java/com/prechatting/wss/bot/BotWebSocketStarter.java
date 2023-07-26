package com.prechatting.wss.bot;

import com.prechatting.ProxyProperties;
import com.prechatting.support.DiscordHelper;
import com.prechatting.wss.WebSocketStarter;
import com.neovisionaries.ws.client.WebSocketFactory;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestConfig;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;


public class BotWebSocketStarter implements WebSocketStarter {
	private final BotMessageListener botMessageListener;
	private final DiscordHelper discordHelper;

	private final ProxyProperties.DiscordConfig discordConfig;

	private final ProxyProperties properties;


	public BotWebSocketStarter(ProxyProperties properties,ProxyProperties.DiscordConfig discordConfig,BotMessageListener botMessageListener,DiscordHelper discordHelper) {
		initProxy(properties);
		this.botMessageListener = botMessageListener;
		this.discordHelper = discordHelper;
		this.discordConfig = discordConfig;
		this.properties = properties;
	}

	@Override
	public void start() throws Exception {
		DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(
				discordConfig.getBotToken(), GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
		builder.addEventListeners(this.botMessageListener);
		WebSocketFactory webSocketFactory = createWebSocketFactory(this.properties);
		builder.setWebsocketFactory(webSocketFactory);
		builder.setSessionController(new CustomSessionController(this.discordHelper.getWss()));
		builder.setRestConfigProvider(value -> new RestConfig().setBaseUrl(this.discordHelper.getServer() + "/api/v10/"));
		builder.build();
	}
}
