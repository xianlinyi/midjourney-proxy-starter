package com.prechatting.wss.handle;


import cn.hutool.core.text.CharSequenceUtil;
import com.prechatting.Constants;
import com.prechatting.enums.MessageType;
import com.prechatting.enums.TaskAction;
import com.prechatting.enums.TaskStatus;
import com.prechatting.support.DiscordChannel;
import com.prechatting.support.DiscordHelper;
import com.prechatting.support.Task;
import com.prechatting.support.TaskCondition;
import com.prechatting.util.ContentParseData;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * blend消息处理.
 * 开始(create): **<https://s.mj.run/JWu6jaL1D-8> <https://s.mj.run/QhfnQY-l68o> --v 5.1** - <@1012983546824114217> (Waiting to start)
 * 进度(update): **<https://s.mj.run/JWu6jaL1D-8> <https://s.mj.run/QhfnQY-l68o> --v 5.1** - <@1012983546824114217> (0%) (relaxed)
 * 完成(create): **<https://s.mj.run/JWu6jaL1D-8> <https://s.mj.run/QhfnQY-l68o> --v 5.1** - <@1012983546824114217> (relaxed)
 */
@Component
public class BlendMessageHandler extends MessageHandler {
	private static final String CONTENT_REGEX = "\\*\\*(.*?)\\*\\* - <@\\d+> \\((.*?)\\)";

	@Override
	public void handle(MessageType messageType, DataObject message, DiscordChannel discordChannel) {
		Optional<DataObject> interaction = message.optObject("interaction");
		String content = getMessageContent(message);
		boolean match = CharSequenceUtil.startWith(content, "**<" + DiscordHelper.SIMPLE_URL_PREFIX) || (interaction.isPresent() && "blend".equals(interaction.get().getString("name")));
		if (!match) {
			return;
		}
		ContentParseData parseData = parse(content);
		if (parseData == null) {
			return;
		}
		if (MessageType.CREATE == messageType) {
			if ("Waiting to start".equals(parseData.getStatus())) {
				// 开始
				List<String> urls = CharSequenceUtil.split(parseData.getPrompt(), " ");
				if (urls.isEmpty()) {
					return;
				}
				String url = getRealUrl(urls.get(0));
				String taskId = this.discordHelper.findTaskIdWithCdnUrl(url);
				TaskCondition condition = new TaskCondition()
						.setId(taskId)
						.setActionSet(Set.of(TaskAction.BLEND))
						.setStatusSet(Set.of(TaskStatus.SUBMITTED));
				Task task = this.taskQueueHelper.findRunningTask(condition).findFirst().orElse(null);
				if (task == null) {
					return;
				}
				task.setProgressMessageId(message.getString("id"));
				task.setPrompt(parseData.getPrompt());
				task.setPromptEn(parseData.getPrompt());
				task.setStatus(TaskStatus.IN_PROGRESS);
				task.awake();
			} else {
				// 完成
				TaskCondition condition = new TaskCondition()
						.setActionSet(Set.of(TaskAction.BLEND))
						.setStatusSet(Set.of(TaskStatus.SUBMITTED, TaskStatus.IN_PROGRESS));
				Task task = this.taskQueueHelper.findRunningTask(condition)
						.max(Comparator.comparing(Task::getProgress))
						.orElse(null);
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
					.setProgressMessageId(message.getString("id"))
					.setActionSet(Set.of(TaskAction.BLEND))
					.setStatusSet(Set.of(TaskStatus.IN_PROGRESS));
			Task task = this.taskQueueHelper.findRunningTask(condition).findFirst().orElse(null);
			if (task == null) {
				return;
			}
			task.setProgressMessageId(message.getString("id"));
			task.setProgress(parseData.getStatus());
			task.setImageUrl(getImageUrl(message));
			task.awake();
		}
	}

	@Override
	public void handle(MessageType messageType, Message message, DiscordChannel discordChannel) {
		String content = message.getContentRaw();
		boolean match = CharSequenceUtil.startWith(content, "**<" + DiscordHelper.SIMPLE_URL_PREFIX) || (message.getInteraction() != null && "blend".equals(message.getInteraction().getName()));
		if (!match) {
			return;
		}
		ContentParseData parseData = parse(content);
		if (parseData == null) {
			return;
		}
		if (MessageType.CREATE == messageType) {
			if ("Waiting to start".equals(parseData.getStatus())) {
				// 开始
				List<String> urls = CharSequenceUtil.split(parseData.getPrompt(), " ");
				if (urls.isEmpty()) {
					return;
				}
				String url = getRealUrl(urls.get(0));
				String taskId = this.discordHelper.findTaskIdWithCdnUrl(url);
				TaskCondition condition = new TaskCondition()
						.setId(taskId)
						.setActionSet(Set.of(TaskAction.BLEND))
						.setStatusSet(Set.of(TaskStatus.SUBMITTED));
				Task task = this.taskQueueHelper.findRunningTask(condition).findFirst().orElse(null);
				if (task == null) {
					return;
				}
				task.setProgressMessageId(message.getId());
				task.setPrompt(parseData.getPrompt());
				task.setPromptEn(parseData.getPrompt());
				task.setStatus(TaskStatus.IN_PROGRESS);
				task.awake();
			} else {
				// 完成
				TaskCondition condition = new TaskCondition()
						.setActionSet(Set.of(TaskAction.BLEND))
						.setStatusSet(Set.of(TaskStatus.SUBMITTED, TaskStatus.IN_PROGRESS));
				Task task = this.taskQueueHelper.findRunningTask(condition)
						.max(Comparator.comparing(Task::getProgress))
						.orElse(null);
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
					.setProgressMessageId(message.getId())
					.setActionSet(Set.of(TaskAction.BLEND))
					.setStatusSet(Set.of(TaskStatus.IN_PROGRESS));
			Task task = this.taskQueueHelper.findRunningTask(condition).findFirst().orElse(null);
			if (task == null) {
				return;
			}
			task.setProgressMessageId( message.getId());
			task.setProgress(parseData.getStatus());
			task.setImageUrl(getImageUrl(message));
			task.awake();
		}
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

	private String getRealUrl(String url) {
		if (CharSequenceUtil.startWith(url, "<" + DiscordHelper.SIMPLE_URL_PREFIX)) {
			return this.discordHelper.getRealUrl(url.substring(1, url.length() - 1));
		}
		return url;
	}

}
