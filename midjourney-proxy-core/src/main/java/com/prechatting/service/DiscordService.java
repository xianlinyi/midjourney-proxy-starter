package com.prechatting.service;


import com.prechatting.ProxyProperties;
import com.prechatting.enums.BlendDimensions;
import com.prechatting.result.Message;
import eu.maxschuster.dataurl.DataUrl;

import java.util.List;

public interface DiscordService {

	Message<Void> imagine(String prompt, ProxyProperties.DiscordConfig discordConfig);

	Message<Void> upscale(String messageId, int index, String messageHash, int messageFlags, ProxyProperties.DiscordConfig discordConfig);

	Message<Void> variation(String messageId, int index, String messageHash, int messageFlags, ProxyProperties.DiscordConfig discordConfig);

	Message<Void> reroll(String messageId, String messageHash, int messageFlags, ProxyProperties.DiscordConfig discordConfig);

	Message<Void> describe(String finalFileName,ProxyProperties.DiscordConfig discordConfig);

	Message<Void> blend(List<String> finalFileNames, BlendDimensions dimensions,ProxyProperties.DiscordConfig discordConfig);

	Message<String> upload(String fileName, DataUrl dataUrl,ProxyProperties.DiscordConfig discordConfig);

	Message<String> sendImageMessage(String content, String finalFileName,ProxyProperties.DiscordConfig discordConfig);

}
