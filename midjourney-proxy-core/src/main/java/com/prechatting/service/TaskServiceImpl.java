package com.prechatting.service;

import com.prechatting.ProxyProperties;
import com.prechatting.ReturnCode;
import com.prechatting.enums.BlendDimensions;
import com.prechatting.result.Message;
import com.prechatting.result.SubmitResultVO;
import com.prechatting.support.Task;
import com.prechatting.support.TaskQueueHelper;
import com.prechatting.util.MimeTypeUtils;
import eu.maxschuster.dataurl.DataUrl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
	private final TaskStoreService taskStoreService;
	private final DiscordService discordService;
	private final TaskQueueHelper taskQueueHelper;

	@Override
	public SubmitResultVO submitImagine(Task task, DataUrl dataUrl, ProxyProperties.DiscordConfig discordConfig) {
		return this.taskQueueHelper.submitTask(task, () -> {
			if (dataUrl != null) {
				String taskFileName = task.getId() + "." + MimeTypeUtils.guessFileSuffix(dataUrl.getMimeType());
				Message<String> uploadResult = this.discordService.upload(taskFileName, dataUrl, discordConfig);
				if (uploadResult.getCode() != ReturnCode.SUCCESS) {
					return Message.of(uploadResult.getCode(), uploadResult.getDescription());
				}
				String finalFileName = uploadResult.getResult();
				Message<String> sendImageResult = this.discordService.sendImageMessage("upload image: " + finalFileName, finalFileName, discordConfig);
				if (sendImageResult.getCode() != ReturnCode.SUCCESS) {
					return Message.of(sendImageResult.getCode(), sendImageResult.getDescription());
				}
				task.setPrompt(sendImageResult.getResult() + " " + task.getPrompt());
				task.setPromptEn(sendImageResult.getResult() + " " + task.getPromptEn());
				task.setDescription("/imagine " + task.getPrompt());
				this.taskStoreService.save(task);
			}
			return this.discordService.imagine(task.getPromptEn(),discordConfig);
		});
	}

	@Override
	public SubmitResultVO submitUpscale(Task task, String targetMessageId, String targetMessageHash, int index, int messageFlags, ProxyProperties.DiscordConfig discordConfig) {
		return this.taskQueueHelper.submitTask(task, () -> this.discordService.upscale(targetMessageId, index, targetMessageHash, messageFlags,discordConfig));
	}

	@Override
	public SubmitResultVO submitVariation(Task task, String targetMessageId, String targetMessageHash, int index, int messageFlags, ProxyProperties.DiscordConfig discordConfig) {
		return this.taskQueueHelper.submitTask(task, () -> this.discordService.variation(targetMessageId, index, targetMessageHash, messageFlags,discordConfig));
	}

	@Override
	public SubmitResultVO submitDescribe(Task task, DataUrl dataUrl, ProxyProperties.DiscordConfig discordConfig) {
		return this.taskQueueHelper.submitTask(task, () -> {
			String taskFileName = task.getId() + "." + MimeTypeUtils.guessFileSuffix(dataUrl.getMimeType());
			Message<String> uploadResult = this.discordService.upload(taskFileName, dataUrl, discordConfig);
			if (uploadResult.getCode() != ReturnCode.SUCCESS) {
				return Message.of(uploadResult.getCode(), uploadResult.getDescription());
			}
			String finalFileName = uploadResult.getResult();
			return this.discordService.describe(finalFileName, discordConfig);
		});
	}

	@Override
	public SubmitResultVO submitBlend(Task task, List<DataUrl> dataUrls, BlendDimensions dimensions, ProxyProperties.DiscordConfig discordConfig) {
		return this.taskQueueHelper.submitTask(task, () -> {
			List<String> finalFileNames = new ArrayList<>();
			for (DataUrl dataUrl : dataUrls) {
				String taskFileName = task.getId() + "." + MimeTypeUtils.guessFileSuffix(dataUrl.getMimeType());
				Message<String> uploadResult = this.discordService.upload(taskFileName, dataUrl, discordConfig);
				if (uploadResult.getCode() != ReturnCode.SUCCESS) {
					return Message.of(uploadResult.getCode(), uploadResult.getDescription());
				}
				finalFileNames.add(uploadResult.getResult());
			}
			return this.discordService.blend(finalFileNames, dimensions, discordConfig);
		});
	}

}
