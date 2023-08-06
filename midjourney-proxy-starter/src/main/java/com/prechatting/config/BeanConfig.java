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
import com.prechatting.support.ChannelPool;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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

@Slf4j
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
	ChannelPool channelPool(DiscordConfigService discordConfigService, ProxyProperties properties) {
		return new ChannelPool(discordConfigService, properties.getDiscord());
	}

	@Bean
	@ConditionalOnBean(ChannelPool.class)
	List<WebSocketStarter> webSocketStarter(ProxyProperties properties, DiscordHelper discordHelper, List<MessageHandler> messageHandlers, ChannelPool channelPool) {
		List<ProxyProperties.DiscordConfig> discords = properties.getDiscord();
		ArrayList<WebSocketStarter> webSocketStarters = new ArrayList<>();
		for (ProxyProperties.DiscordConfig discord : discords) {
			if (discord.isUserWss()) {
				UserMessageListener userMessageListener = new UserMessageListener(discord,messageHandlers,channelPool);
				webSocketStarters.add(new UserWebSocketStarter(properties,discord,userMessageListener,discordHelper,channelPool));
			}else {
				BotMessageListener botMessageListener = new BotMessageListener(discord,messageHandlers,channelPool);
				webSocketStarters.add(new BotWebSocketStarter(properties,discord,botMessageListener,discordHelper,channelPool));
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
	ApplicationRunner enableMetaChangeReceiverInitializer(List<WebSocketStarter> webSocketStarters,ChannelPool channelPool) {
		return args -> {
			log.info("[midjourney-proxy-starter] > 正在初始化与discord的连接...");
			for (WebSocketStarter socketStarter : webSocketStarters) {
				try {
					socketStarter.start();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			// Wait for all WebSocketStarters to be ready
			boolean allReady = false;
			while (!allReady) {
				allReady = true;
				for (WebSocketStarter socketStarter : webSocketStarters) {
					if (!socketStarter.isReady()) {
						allReady = false;
						break;
					}
				}
				Thread.sleep(100); // Add a short delay to avoid busy-waiting
			}


			log.info("[midjourney-proxy-starter] > 与discord的连接初始化完成，共获取到 channel：{} 个", channelPool.getChannelCount());
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
