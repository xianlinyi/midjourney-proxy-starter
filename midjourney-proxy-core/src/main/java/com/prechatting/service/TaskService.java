package com.prechatting.service;

import com.prechatting.ProxyProperties;
import com.prechatting.enums.BlendDimensions;
import com.prechatting.result.SubmitResultVO;
import com.prechatting.support.Task;
import eu.maxschuster.dataurl.DataUrl;

import java.util.List;

public interface TaskService {

	SubmitResultVO submitImagine(Task task, DataUrl dataUrl, ProxyProperties.DiscordConfig discordConfig);

	SubmitResultVO submitUpscale(Task task, String targetMessageId, String targetMessageHash, int index,  int messageFlags, ProxyProperties.DiscordConfig discordConfig);

	SubmitResultVO submitVariation(Task task, String targetMessageId, String targetMessageHash, int index, int messageFlags, ProxyProperties.DiscordConfig discordConfig);

	SubmitResultVO submitDescribe(Task task, DataUrl dataUrl, ProxyProperties.DiscordConfig discordConfig);

	SubmitResultVO submitBlend(Task task, List<DataUrl> dataUrls, BlendDimensions dimensions, ProxyProperties.DiscordConfig discordConfig);
}