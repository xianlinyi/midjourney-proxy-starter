package com.prechatting.config;

import com.prechatting.ProxyProperties;
import com.prechatting.service.DefaultDiscordConfigService;
import com.prechatting.service.DiscordConfigService;
import com.prechatting.service.TaskStoreService;
import com.prechatting.service.TranslateService;
import com.prechatting.service.store.InMemoryTaskStoreServiceImpl;
import com.prechatting.service.store.RedisTaskStoreServiceImpl;
import com.prechatting.service.translate.BaiduTranslateServiceImpl;
import com.prechatting.service.translate.GPTTranslateServiceImpl;
import com.prechatting.support.DiscordHelper;
import com.prechatting.support.Task;
import com.prechatting.support.TaskMixin;
import com.prechatting.wss.WebSocketStarter;
import com.prechatting.wss.bot.BotMessageListener;
import com.prechatting.wss.bot.BotWebSocketStarter;
import com.prechatting.wss.handle.BlendMessageHandler;
import com.prechatting.wss.handle.MessageHandler;
import com.prechatting.wss.user.UserMessageListener;
import com.prechatting.wss.user.UserWebSocketStarter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class BeanConfig {

	@Bean
	TranslateService translateService(ProxyProperties properties) {
		return switch (properties.getTranslateWay()) {
			case BAIDU -> new BaiduTranslateServiceImpl(properties.getBaiduTranslate());
			case GPT -> new GPTTranslateServiceImpl(properties);
			default -> prompt -> prompt;
		};
	}

	@Bean
	TaskStoreService taskStoreService(ProxyProperties proxyProperties, RedisConnectionFactory redisConnectionFactory) {
		ProxyProperties.TaskStore.Type type = proxyProperties.getTaskStore().getType();
		Duration timeout = proxyProperties.getTaskStore().getTimeout();
		return switch (type) {
			case IN_MEMORY -> new InMemoryTaskStoreServiceImpl(timeout);
			case REDIS -> new RedisTaskStoreServiceImpl(timeout, taskRedisTemplate(redisConnectionFactory));
		};
	}

	@Bean
	RedisTemplate<String, Task> taskRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Task> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Task.class));
		return redisTemplate;
	}

	@Bean
	List<WebSocketStarter> webSocketStarter(ProxyProperties properties, DiscordHelper discordHelper,List<MessageHandler> messageHandlers) {
		List<ProxyProperties.DiscordConfig> discords = properties.getDiscord();
		ArrayList<WebSocketStarter> webSocketStarters = new ArrayList<>();
		for (ProxyProperties.DiscordConfig discord : discords) {
			if (discord.isUserWss()) {
				UserMessageListener userMessageListener = new UserMessageListener(discord,messageHandlers);
				webSocketStarters.add(new UserWebSocketStarter(properties,discord,userMessageListener,discordHelper));
			}else {
				BotMessageListener botMessageListener = new BotMessageListener(discord,messageHandlers);

				webSocketStarters.add(new BotWebSocketStarter(properties,discord,botMessageListener,discordHelper));
			}

		}
		return webSocketStarters;
	}

	@Bean
	@ConditionalOnMissingBean(DiscordConfigService.class)
	DiscordConfigService discordConfigService() {
		return new DefaultDiscordConfigService();
	}

	@Bean
	DiscordHelper discordHelper(ProxyProperties properties) {
		return new DiscordHelper(properties);
	}


	@Bean
	ApplicationRunner enableMetaChangeReceiverInitializer(List<WebSocketStarter> webSocketStarter) {
		return args -> {
			for (WebSocketStarter socketStarter : webSocketStarter) {
				socketStarter.start();
			}
		};
	}

	@Bean
	Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer(ProxyProperties properties) {
		if (properties.isIncludeTaskExtended()) {
			return builder -> {
			};
		}
		return builder -> builder.mixIn(Task.class, TaskMixin.class);
	}

}
