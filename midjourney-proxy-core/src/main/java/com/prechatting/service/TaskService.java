package com.prechatting.service;

import com.prechatting.enums.BlendDimensions;
import com.prechatting.result.SubmitResultVO;
import com.prechatting.support.DiscordChannel;
import com.prechatting.support.Task;
import eu.maxschuster.dataurl.DataUrl;

import java.util.List;

public interface TaskService {

	SubmitResultVO submitImagine(Task task, DataUrl dataUrl);

	SubmitResultVO submitUpscale(Task task, String targetMessageId, String targetMessageHash, int index, int messageFlags);

	SubmitResultVO submitVariation(Task task, String targetMessageId, String targetMessageHash, int index, int messageFlags);

	SubmitResultVO submitDescribe(Task task, DataUrl dataUrl);

	SubmitResultVO submitBlend(Task task, List<DataUrl> dataUrls, BlendDimensions dimensions);
}