package com.prechatting.wss.handle;


import com.prechatting.Constants;
import com.prechatting.enums.MessageType;
import com.prechatting.enums.TaskAction;
import com.prechatting.enums.TaskStatus;
import com.prechatting.support.DiscordChannel;
import com.prechatting.support.Task;
import com.prechatting.support.TaskCondition;
import com.prechatting.util.ContentParseData;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * imagine消息处理.
 * 开始(create): **cat** - <@1012983546824114217> (Waiting to start)
 * 进度(update): **cat** - <@1012983546824114217> (0%) (relaxed)
 * 完成(create): **cat** - <@1012983546824114217> (relaxed)
 */
@Slf4j
@Component
public class ImagineMessageHandler extends MessageHandler {
	private static final String CONTENT_REGEX = "\\*\\*(.*?)\\*\\* - <@\\d+> \\((.*?)\\)";

	@Override
	public void handle(MessageType messageType, DataObject message, DiscordChannel discordChannel) {
		String content = getMessageContent(message);
		ContentParseData parseData = parse(content);
		if (parseData == null) {
			return;
		}
		if (MessageType.CREATE == messageType) {
			if ("Waiting to start".equals(parseData.getStatus())) {
				// 开始
				TaskCondition condition = new TaskCondition()
						.setDiscordChannel(discordChannel)
						.setActionSet(Set.of(TaskAction.IMAGINE))
						.setStatusSet(Set.of(TaskStatus.SUBMITTED));
				Task task = this.taskQueueHelper.findRunningTask(taskPredicate(condition))
						.findFirst().orElse(null);
				if (task == null) {
					return;
				}
				task.setProgressMessageId(message.getString("id"));
				task.setProperty(Constants.TASK_PROPERTY_FINAL_PROMPT, parseData.getPrompt());
				task.setStatus(TaskStatus.IN_PROGRESS);
				task.awake();
			} else {
				// 完成
				channelPool.finish(discordChannel,TaskAction.IMAGINE);
				log.debug("imagine任务完成, message: {}", message);
				TaskCondition condition = new TaskCondition()
						.setDiscordChannel(discordChannel)
						.setActionSet(Set.of(TaskAction.IMAGINE))
						.setStatusSet(Set.of(TaskStatus.SUBMITTED, TaskStatus.IN_PROGRESS));
				Task task = this.taskQueueHelper.findRunningTask(taskPredicate(condition))
						.findFirst().orElse(null);
				log.debug("task: {}", task);
				if (task == null) {
					return;
				}
				task.setProperty(Constants.TASK_PROPERTY_FINAL_PROMPT, parseData.getPrompt());
				finishTask(task, message, discordChannel);
				task.awake();
			}
		} else if (MessageType.UPDATE == messageType) {
			// 进度
			TaskCondition condition = new TaskCondition()
					.setDiscordChannel(discordChannel)
					.setActionSet(Set.of(TaskAction.IMAGINE))
					.setStatusSet(Set.of(TaskStatus.SUBMITTED, TaskStatus.IN_PROGRESS));
			Task task = this.taskQueueHelper.findRunningTask(taskPredicate(condition))
					.findFirst().orElse(null);
			if (task == null) {
				return;
			}
			task.setProgressMessageId( message.getString("id"));
			task.setProperty(Constants.TASK_PROPERTY_FINAL_PROMPT, parseData.getPrompt());
			task.setStatus(TaskStatus.IN_PROGRESS);
			task.setProgress(parseData.getStatus());
			task.setImageUrl(getImageUrl(message));
			task.awake();
		}
	}

	@Override
	public void handle(MessageType messageType, Message message, DiscordChannel discordChannel) {
		String content = message.getContentRaw();
		ContentParseData parseData = parse(content);
		if (parseData == null) {
			return;
		}
		String realPrompt = this.discordHelper.getRealPrompt(parseData.getPrompt());
		if (MessageType.CREATE == messageType) {
			if ("Waiting to start".equals(parseData.getStatus())) {
				// 开始
				TaskCondition condition = new TaskCondition()
						.setDiscordChannel(discordChannel)
						.setActionSet(Set.of(TaskAction.IMAGINE))
						.setStatusSet(Set.of(TaskStatus.SUBMITTED));
				Task task = this.taskQueueHelper.findRunningTask(taskPredicate(condition))
						.findFirst().orElse(null);
				if (task == null) {
					return;
				}
				task.setProgressMessageId(message.getId());
				task.setProperty(Constants.TASK_PROPERTY_FINAL_PROMPT, parseData.getPrompt());
				task.setStatus(TaskStatus.IN_PROGRESS);
				task.awake();
			} else {
				// 完成
				TaskCondition condition = new TaskCondition()
						.setDiscordChannel(discordChannel)
						.setActionSet(Set.of(TaskAction.IMAGINE))
						.setStatusSet(Set.of(TaskStatus.SUBMITTED, TaskStatus.IN_PROGRESS));
				Task task = this.taskQueueHelper.findRunningTask(taskPredicate(condition))
						.findFirst().orElse(null);
				if (task == null) {
					return;
				}
				task.setProperty(Constants.TASK_PROPERTY_FINAL_PROMPT, parseData.getPrompt());
				finishTask(task, message, discordChannel);
				task.awake();
			}
		} else if (MessageType.UPDATE == messageType) {
			// 进度
			TaskCondition condition = new TaskCondition()
					.setDiscordChannel(discordChannel)
					.setActionSet(Set.of(TaskAction.IMAGINE))
					.setStatusSet(Set.of(TaskStatus.SUBMITTED, TaskStatus.IN_PROGRESS));
			Task task = this.taskQueueHelper.findRunningTask(taskPredicate(condition))
					.findFirst().orElse(null);
			if (task == null) {
				return;
			}
			task.setProgressMessageId(message.getId());
			task.setProperty(Constants.TASK_PROPERTY_FINAL_PROMPT, parseData.getPrompt());
			task.setStatus(TaskStatus.IN_PROGRESS);
			task.setProgress(parseData.getStatus());
			task.setImageUrl(getImageUrl(message));
			task.awake();
		}
	}

	private Predicate<Task> taskPredicate(TaskCondition condition) {
		return condition.and(t -> {
			return t.getDiscordChannel().getUserToken().equals(condition.getDiscordChannel().getUserToken())
					&& t.getDiscordChannel().getChannelId().equals(condition.getDiscordChannel().getChannelId())
					&& t.getDiscordChannel().getGuildId().equals(condition.getDiscordChannel().getGuildId());
					//&& t.getAction().equals(condition.getActionSet());
		});
	}

	private ContentParseData parse(String content) {
		Matcher matcher = Pattern.compile(CONTENT_REGEX).matcher(content);
		if (!matcher.find()) {
			return null;
		}
		ContentParseData parseData = new ContentParseData();
		parseData.setPrompt(matcher.group(1));
		parseData.setStatus(matcher.group(2));
		return parseData;
	}

}
