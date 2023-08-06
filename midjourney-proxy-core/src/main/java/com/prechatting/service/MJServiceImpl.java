package com.prechatting.service;

import cn.hutool.core.comparator.CompareUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.RandomUtil;
import com.prechatting.Constants;
import com.prechatting.ProxyProperties;
import com.prechatting.ReturnCode;
import com.prechatting.dto.*;
import com.prechatting.enums.TaskAction;
import com.prechatting.enums.TaskStatus;
import com.prechatting.result.SubmitResultVO;
import com.prechatting.service.listener.DefaultProgressListener;
import com.prechatting.service.listener.IProgressListener;
import com.prechatting.support.ChannelPool;
import com.prechatting.support.Task;
import com.prechatting.support.TaskCondition;
import com.prechatting.support.TaskQueueHelper;
import com.prechatting.util.ConvertUtils;
import com.prechatting.util.MimeTypeUtils;
import com.prechatting.util.SnowFlake;
import com.prechatting.util.TaskChangeParams;
import eu.maxschuster.dataurl.DataUrl;
import eu.maxschuster.dataurl.DataUrlSerializer;
import eu.maxschuster.dataurl.IDataUrlSerializer;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


import java.net.MalformedURLException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MJServiceImpl implements MJService {
    private final TranslateService translateService;
    private final TaskStoreService taskStoreService;
    private final TaskQueueHelper taskQueueHelper;
    private final ProxyProperties properties;
    private final TaskService taskService;

    public SubmitResultVO imagine(SubmitImagineDTO imagineDTO){
        DefaultProgressListener defaultProgressListener = new DefaultProgressListener();
        return doImagine(imagineDTO, defaultProgressListener);
    }

    public SubmitResultVO imagine(SubmitImagineDTO imagineDTO, IProgressListener listener){
        return doImagine(imagineDTO, listener);
    }

    private SubmitResultVO doImagine(SubmitImagineDTO imagineDTO, IProgressListener listener) {
        String prompt = imagineDTO.getPrompt();
        if (CharSequenceUtil.isBlank(prompt)) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "prompt不能为空");
        }
        prompt = prompt.trim();
        Task task = newTask(imagineDTO);
        task.setNonce(SnowFlake.INSTANCE.nextId());
        task.setListener(listener);
        task.setAction(TaskAction.IMAGINE);
        task.setPrompt(prompt);
        String promptEn;
        int paramStart = prompt.indexOf(" --");
        if (paramStart > 0) {
            promptEn = this.translateService.translateToEnglish(prompt.substring(0, paramStart)).trim() + prompt.substring(paramStart);
        } else {
            promptEn = this.translateService.translateToEnglish(prompt).trim();
        }
        if (CharSequenceUtil.isBlank(promptEn)) {
            promptEn = prompt;
        }
//        // 放弃敏感词检测
//		if (BannedPromptUtils.isBanned(promptEn)) {
//			return SubmitResultVO.fail(ReturnCode.BANNED_PROMPT, "可能包含敏感词");
//		}
        DataUrl dataUrl = null;
        if (CharSequenceUtil.isNotBlank(imagineDTO.getBase64())) {
            IDataUrlSerializer serializer = new DataUrlSerializer();
            try {
                dataUrl = serializer.unserialize(imagineDTO.getBase64());
            } catch (MalformedURLException e) {
                return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "basisImageBase64格式错误");
            }
        }
        task.setPromptEn(promptEn);
        task.setDescription("/imagine " + prompt);
        return this.taskService.submitImagine(task, dataUrl);
    }

    public SubmitResultVO simpleChange(SubmitSimpleChangeDTO simpleChangeDTO) {
        TaskChangeParams changeParams = ConvertUtils.convertChangeParams(simpleChangeDTO.getContent());
        if (changeParams == null) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "content参数错误");
        }
        SubmitChangeDTO changeDTO = new SubmitChangeDTO();
        changeDTO.setAction(changeParams.getAction());
        changeDTO.setTaskId(changeParams.getId());
        changeDTO.setIndex(changeParams.getIndex());
        changeDTO.setState(simpleChangeDTO.getState());
        changeDTO.setNotifyHook(simpleChangeDTO.getNotifyHook());
        return change(changeDTO);
    }


    public SubmitResultVO change(SubmitChangeDTO changeDTO) {
        return change(changeDTO, new DefaultProgressListener());
    }

    public SubmitResultVO change(SubmitChangeDTO changeDTO, IProgressListener listener) {
        if (CharSequenceUtil.isBlank(changeDTO.getTaskId())) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "taskId不能为空");
        }
        if (!Set.of(TaskAction.UPSCALE, TaskAction.VARIATION, TaskAction.REROLL, TaskAction.REMIX).contains(changeDTO.getAction())) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "action参数错误");
        }
        String description = "/up " + changeDTO.getTaskId();
        if (TaskAction.REROLL.equals(changeDTO.getAction())) {
            description += " R";
        } else {
            description += " " + changeDTO.getAction().name().charAt(0) + changeDTO.getIndex();
        }
        TaskCondition condition = new TaskCondition().setDescription(description);
        Task existTask = this.taskStoreService.findOne(condition);
        if (existTask != null ) {
            return SubmitResultVO.of(ReturnCode.EXISTED, "任务已存在且正在进行", existTask.getId())
                    .setProperty("status", existTask.getStatus())
                    .setProperty("imageUrl", existTask.getImageUrl());
        }
        Task targetTask;
        if (null == changeDTO.getTask()){
            targetTask = this.taskStoreService.get(changeDTO.getTaskId());
        }else {
            targetTask = changeDTO.getTask();
        }
        if (targetTask == null) {
            return SubmitResultVO.fail(ReturnCode.NOT_FOUND, "关联任务不存在或已失效");
        }
        if (!TaskStatus.SUCCESS.equals(targetTask.getStatus())) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "关联任务状态错误");
        }
        if (!Set.of(TaskAction.IMAGINE, TaskAction.VARIATION, TaskAction.BLEND).contains(targetTask.getAction())) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "关联任务不允许执行变化");
        }
        Task task = newTask(changeDTO);
        task.setNonce(SnowFlake.INSTANCE.nextId());
        task.setListener(listener);
        task.setAction(changeDTO.getAction());
        task.setPrompt(targetTask.getPrompt());
        task.setPromptEn(targetTask.getPromptEn());
        task.setMessageId(targetTask.getMessageId());
        task.setProgressMessageId(targetTask.getProgressMessageId());
        task.setFlags(targetTask.getFlags());
        task.setMessageHash(targetTask.getMessageHash());
        task.setDiscordChannel(targetTask.getDiscordChannel());
        task.setProperty(Constants.TASK_PROPERTY_FINAL_PROMPT, targetTask.getProperty(Constants.TASK_PROPERTY_FINAL_PROMPT));
        task.setDescription(description);
        task.setRemixPrompt(changeDTO.getRemixPrompt());

        if (TaskAction.UPSCALE.equals(changeDTO.getAction())) {
            return this.taskService.submitUpscale(task, changeDTO.getIndex());
        } else if (TaskAction.VARIATION.equals(changeDTO.getAction())) {
            return this.taskService.submitRemix(task, changeDTO.getIndex());
        } else if (TaskAction.REMIX.equals(changeDTO.getAction())){
            return this.taskService.submitRemix(task, changeDTO.getIndex());
        } else {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "不支持的操作: " + changeDTO.getAction());
        }
    }

    public SubmitResultVO describe(SubmitDescribeDTO describeDTO) {
        if (CharSequenceUtil.isBlank(describeDTO.getBase64())) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "base64不能为空");
        }
        IDataUrlSerializer serializer = new DataUrlSerializer();
        DataUrl dataUrl;
        try {
            dataUrl = serializer.unserialize(describeDTO.getBase64());
        } catch (MalformedURLException e) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "base64格式错误");
        }
        Task task = newTask(describeDTO);
        task.setAction(TaskAction.DESCRIBE);
        String taskFileName = task.getId() + "." + MimeTypeUtils.guessFileSuffix(dataUrl.getMimeType());
        task.setDescription("/describe " + taskFileName);
        return this.taskService.submitDescribe(task, dataUrl);
    }

    public SubmitResultVO blend(SubmitBlendDTO blendDTO) {
        List<String> base64Array = blendDTO.getBase64Array();
        if (base64Array == null || base64Array.size() < 2 || base64Array.size() > 5) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "base64List参数错误");
        }
        if (blendDTO.getDimensions() == null) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "dimensions参数错误");
        }
        IDataUrlSerializer serializer = new DataUrlSerializer();
        List<DataUrl> dataUrlList = new ArrayList<>();
        try {
            for (String base64 : base64Array) {
                DataUrl dataUrl = serializer.unserialize(base64);
                dataUrlList.add(dataUrl);
            }
        } catch (MalformedURLException e) {
            return SubmitResultVO.fail(ReturnCode.VALIDATION_ERROR, "base64格式错误");
        }
        Task task = newTask(blendDTO);
        task.setAction(TaskAction.BLEND);
        task.setDescription("/blend " + task.getId() + " " + dataUrlList.size());
        return this.taskService.submitBlend(task, dataUrlList, blendDTO.getDimensions());
    }


    public List<Task> list() {
        return this.taskStoreService.list().stream()
                .sorted((t1, t2) -> CompareUtil.compare(t2.getSubmitTime(), t1.getSubmitTime()))
                .toList();
    }


    public Task fetch(String id) {
        return this.taskStoreService.get(id);
    }


    public List<Task> queue() {
        Set<String> queueTaskIds = this.taskQueueHelper.getQueueTaskIds();
        return queueTaskIds.stream().map(this.taskStoreService::get).filter(Objects::nonNull)
                .sorted(Comparator.comparing(Task::getSubmitTime))
                .toList();
    }


    public List<Task> listByCondition(@RequestBody TaskConditionDTO conditionDTO) {
        if (conditionDTO.getIds() == null) {
            return Collections.emptyList();
        }
        return conditionDTO.getIds().stream().map(this.taskStoreService::get).filter(Objects::nonNull).toList();
    }

    private Task newTask(BaseSubmitDTO base) {
        Task task = new Task();
        task.setId(RandomUtil.randomNumbers(16));
        task.setSubmitTime(System.currentTimeMillis());
        task.setState(base.getState());
        String notifyHook = CharSequenceUtil.isBlank(base.getNotifyHook()) ? this.properties.getNotifyHook() : base.getNotifyHook();
        task.setProperty(Constants.TASK_PROPERTY_NOTIFY_HOOK, notifyHook);
        return task;
    }
}
