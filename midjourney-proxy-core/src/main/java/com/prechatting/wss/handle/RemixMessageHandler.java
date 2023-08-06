package com.prechatting.wss.handle;

import cn.hutool.core.text.CharSequenceUtil;
import com.prechatting.enums.MessageType;
import com.prechatting.enums.TaskAction;
import com.prechatting.enums.TaskStatus;
import com.prechatting.support.DiscordChannel;
import com.prechatting.support.Task;
import com.prechatting.support.TaskCondition;
import com.prechatting.util.UVContentParseData;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RemixMessageHandler extends MessageHandler{
    private static final String START_CONTENT_REGEX = "Remixing image with prompt \\*\\*(.*?)\\*\\* - <@\\d+> \\((.*?)\\)";
    private static final String CONTENT_REGEX = "\\*\\*(.*?)\\*\\* - Remix by <@\\d+> \\((.*?)\\)";

    @Override
    public void handle(MessageType messageType, DataObject message, DiscordChannel discordChannel) {
        String content = getMessageContent(message);
        if (MessageType.CREATE.equals(messageType)){
            UVContentParseData start = parseStart(content);
            if (start != null) {
                // 开始
                Task task = this.taskQueueHelper.getRunningTaskByNonce(message.getString("nonce"));
                if (task == null) {
                    return;
                }
                task.setProgressMessageId(message.getString("id"));
                task.setStatus(TaskStatus.IN_PROGRESS);
                task.awake();
                return;
            }else {
                // 结束
                UVContentParseData end = parse(content,CONTENT_REGEX);
                if (end == null) {
                    return;
                }
                TaskCondition condition = new TaskCondition()
                        .setFinalPromptEn(end.getPrompt())
                        .setActionSet(Set.of(TaskAction.REMIX))
                        .setStatusSet(Set.of(TaskStatus.SUBMITTED, TaskStatus.IN_PROGRESS));
                Task task = this.taskQueueHelper.findRunningTask(condition).findAny()
                        .orElse(null);
                if (task == null) {
                    return;
                }
                finishTask(task, message, discordChannel);
                task.awake();
            }
        }else if (MessageType.UPDATE == messageType){
            // 进度
            UVContentParseData parseData = parse(content,CONTENT_REGEX);
            if (parseData == null || CharSequenceUtil.equalsAny(parseData.getStatus(), "relaxed", "fast")) {
                return;
            }
            TaskCondition condition = new TaskCondition()
                    .setProgressMessageId(message.getString("id"))
                    .setActionSet(Set.of(TaskAction.REMIX))
                    .setStatusSet(Set.of(TaskStatus.SUBMITTED, TaskStatus.IN_PROGRESS));
            Task task = this.taskQueueHelper.findRunningTask(condition)
                    .findFirst().orElse(null);
            if (task == null) {
                return;
            }
            task.setProgressMessageId(message.getString("id"));
            task.setStatus(TaskStatus.IN_PROGRESS);
            task.setProgress(parseData.getStatus());
            task.setImageUrl(getImageUrl(message));
            task.awake();
        }
    }

    @Override
    public void handle(MessageType messageType, Message message, DiscordChannel discordChannel) {

    }

    private UVContentParseData parseStart(String content) {
        Matcher matcher = Pattern.compile(START_CONTENT_REGEX).matcher(content);
        if (!matcher.find()) {
            return null;
        }
        UVContentParseData parseData = new UVContentParseData();
        parseData.setPrompt(matcher.group(1));
        parseData.setStatus(matcher.group(2));
        return parseData;
    }

    private static UVContentParseData parse(String content, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(content);
        if (!matcher.find()) {
            return null;
        }
        UVContentParseData parseData = new UVContentParseData();
        parseData.setPrompt(matcher.group(1));
        parseData.setStatus(matcher.group(2));
        return parseData;
    }

//    public static void main(String[] args) {
//        String content = "**white UI of the app** - Remix by <@1019172871567855647> (relaxed)";
//        UVContentParseData parse = parse(content, CONTENT_REGEX);
//        log.debug("parse: {}", parse);
//    }
}
