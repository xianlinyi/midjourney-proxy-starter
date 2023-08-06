package com.prechatting.service;


import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.prechatting.Constants;
import com.prechatting.ProxyProperties;
import com.prechatting.ReturnCode;
import com.prechatting.enums.BlendDimensions;
import com.prechatting.result.Message;
import com.prechatting.support.DiscordChannel;
import com.prechatting.support.DiscordHelper;
import com.prechatting.support.Task;
import com.prechatting.util.SnowFlake;
import eu.maxschuster.dataurl.DataUrl;
import io.netty.util.internal.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordServiceImpl implements DiscordService {
	private final ProxyProperties properties;
	private final DiscordHelper discordHelper;
	private final DiscordConfigService discordConfigService;

	private String discordApiUrl;

	private String imagineParamsJson;
	private String upscaleParamsJson;
	private String variationParamsJson;
	private String rerollParamsJson;
	private String describeParamsJson;
	private String blendParamsJson;
	private String messageParamsJson;
	private String remixParamsJson;


	@PostConstruct
	void init() {

		String serverUrl = this.discordHelper.getServer();
		this.discordApiUrl = serverUrl + "/api/v9/interactions";

		this.imagineParamsJson = ResourceUtil.readUtf8Str("api-params/imagine.json");
		this.upscaleParamsJson = ResourceUtil.readUtf8Str("api-params/upscale.json");
		this.variationParamsJson = ResourceUtil.readUtf8Str("api-params/variation.json");
		this.rerollParamsJson = ResourceUtil.readUtf8Str("api-params/reroll.json");
		this.describeParamsJson = ResourceUtil.readUtf8Str("api-params/describe.json");
		this.blendParamsJson = ResourceUtil.readUtf8Str("api-params/blend.json");
		this.messageParamsJson = ResourceUtil.readUtf8Str("api-params/message.json");
		this.remixParamsJson = ResourceUtil.readUtf8Str("api-params/remix.json");
	}


	@Override
	public Message<Void> imagine(String prompt, DiscordChannel discordChannel) {
		log.debug("imagine: {}，userToken:{}, guildId:{}, channelId:{}", prompt, discordChannel.getUserToken(), discordChannel.getGuildId(), discordChannel.getChannelId());
		String paramsStr = this.imagineParamsJson.replace("$guild_id", discordChannel.getGuildId())
				.replace("$channel_id", discordChannel.getChannelId())
				.replace("$session_id", discordChannel.getSessionId())
				.replace("$nonce", SnowFlake.INSTANCE.nextId());
		JSONObject params = new JSONObject(paramsStr);
		params.getJSONObject("data").getJSONArray("options").getJSONObject(0)
				.put("value", prompt);
		return postJsonAndCheckStatus(params.toString(),discordChannel);
	}

	@Override
	public Message<Void> upscale(Task task, int index,DiscordChannel discordChannel) {
		String paramsStr = this.upscaleParamsJson.replace("$guild_id", discordChannel.getGuildId())
				.replace("$channel_id", discordChannel.getChannelId())
				.replace("$session_id", discordChannel.getSessionId())
				.replace("$message_id", task.getMessageId())
				.replace("$nonce", task.getNonce())
				.replace("$index", String.valueOf(index))
				.replace("$message_hash", task.getMessageHash());
		paramsStr = new JSONObject(paramsStr).put("message_flags", task.getFlags()).toString();
		return postJsonAndCheckStatus(paramsStr,discordChannel);
	}

	@Override
	public Message<Void> variation(Task task, int index, DiscordChannel discordChannel) {
		String paramsStr = this.variationParamsJson.replace("$guild_id", discordChannel.getGuildId())
				.replace("$channel_id", discordChannel.getChannelId())
				.replace("$session_id", discordChannel.getSessionId())
				.replace("$index", String.valueOf(index))
				.replace("$nonce", task.getNonce())
				.replace("$message_id",task.getMessageId())
				.replace("$message_hash", task.getMessageHash());
		paramsStr = new JSONObject(paramsStr).put("message_flags", task.getFlags()).toString();
		return postJsonAndCheckStatus(paramsStr,discordChannel);
	}

	@Override
	public Message<Void> remix(Task task, int index, DiscordChannel discordChannel) {
		log.debug("[midjouney-proxy-starter] > 当前channel为 {} ",discordChannel);
		variation(task,index,discordChannel);
		log.debug("[midjouney-proxy-starter] > 等待返回remix组件id");
		while (StringUtil.isNullOrEmpty(task.getComponentId())) {
			synchronized (task) {
				try {
					task.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		log.debug("[midjouney-proxy-starter] > remix组件id为 {} ",task.getComponentId());
		log.debug("[midjouney-proxy-starter] > 调用remix操作");
		task.setNonce(SnowFlake.INSTANCE.nextId());
		String paramsStr = this.remixParamsJson.replace("$guild_id", discordChannel.getGuildId())
				.replace("$channel_id", discordChannel.getChannelId())
				.replace("$session_id", discordChannel.getSessionId())
				.replace("$message_hash", task.getMessageHash())
				.replace("$index", String.valueOf(index))
				.replace("$nonce", task.getNonce())
				.replace("$component_id", task.getComponentId())
				.replace("$prompt", task.getRemixPrompt());
		return postJsonAndCheckStatus(paramsStr,discordChannel);
	}


	@Override
	public Message<Void> reroll(String messageId, String messageHash, int messageFlags, DiscordChannel discordChannel) {
		
		String paramsStr = this.rerollParamsJson.replace("$guild_id", discordChannel.getGuildId())
				.replace("$channel_id", discordChannel.getChannelId())
				.replace("$session_id", discordChannel.getSessionId())
				.replace("$message_id", messageId)
				.replace("$message_hash", messageHash);
		paramsStr = new JSONObject(paramsStr).put("message_flags", messageFlags).toString();
		return postJsonAndCheckStatus(paramsStr,discordChannel);
	}

	@Override
	public Message<Void> describe(String finalFileName, DiscordChannel discordChannel) {

		String fileName = CharSequenceUtil.subAfter(finalFileName, "/", true);
		String paramsStr = this.describeParamsJson.replace("$guild_id", discordChannel.getGuildId())
				.replace("$channel_id", discordChannel.getChannelId())
				.replace("$session_id", discordChannel.getSessionId())
				.replace("$file_name", fileName)
				.replace("$final_file_name", finalFileName);
		return postJsonAndCheckStatus(paramsStr,discordChannel);
	}

	@Override
	public Message<Void> blend(List<String> finalFileNames, BlendDimensions dimensions, DiscordChannel discordChannel) {
		String paramsStr = this.blendParamsJson.replace("$guild_id", discordChannel.getGuildId())
				.replace("$channel_id", discordChannel.getChannelId())
				.replace("$session_id", discordChannel.getSessionId());
		JSONObject params = new JSONObject(paramsStr);
		JSONArray options = params.getJSONObject("data").getJSONArray("options");
		JSONArray attachments = params.getJSONObject("data").getJSONArray("attachments");
		for (int i = 0; i < finalFileNames.size(); i++) {
			String finalFileName = finalFileNames.get(i);
			String fileName = CharSequenceUtil.subAfter(finalFileName, "/", true);
			JSONObject attachment = new JSONObject().put("id", String.valueOf(i))
					.put("filename", fileName)
					.put("uploaded_filename", finalFileName);
			attachments.put(attachment);
			JSONObject option = new JSONObject().put("type", 11)
					.put("name", "image" + (i + 1))
					.put("value", i);
			options.put(option);
		}
		options.put(new JSONObject().put("type", 3)
				.put("name", "dimensions")
				.put("value", "--ar " + dimensions.getValue()));
		return postJsonAndCheckStatus(params.toString(),discordChannel);
	}

	@Override
	public Message<String> upload(String fileName, DataUrl dataUrl, DiscordChannel discordChannel) {
		try {
			String discordUploadUrl = this.discordHelper.getServer() + "/api/v9/channels/" + discordChannel.getChannelId() + "/attachments";
			JSONObject fileObj = new JSONObject();
			fileObj.put("filename", fileName);
			fileObj.put("file_size", dataUrl.getData().length);
			fileObj.put("id", "0");
			JSONObject params = new JSONObject()
					.put("files", new JSONArray().put(fileObj));
			ResponseEntity<String> responseEntity = postJson(discordUploadUrl, params.toString(), discordChannel);
			if (responseEntity.getStatusCode() != HttpStatus.OK) {
				log.error("上传图片到discord失败, status: {}, msg: {}", responseEntity.getStatusCodeValue(), responseEntity.getBody());
				return Message.of(ReturnCode.VALIDATION_ERROR, "上传图片到discord失败");
			}
			JSONArray array = new JSONObject(responseEntity.getBody()).getJSONArray("attachments");
			if (array.length() == 0) {
				return Message.of(ReturnCode.VALIDATION_ERROR, "上传图片到discord失败");
			}
			String uploadUrl = array.getJSONObject(0).getString("upload_url");
			String uploadFilename = array.getJSONObject(0).getString("upload_filename");
			putFile(uploadUrl, dataUrl, discordChannel);
			return Message.success(uploadFilename);
		} catch (Exception e) {
			log.error("上传图片到discord失败", e);
			return Message.of(ReturnCode.FAILURE, "上传图片到discord失败");
		}
	}

	@Override
	public Message<String> sendImageMessage(String content, String finalFileName, DiscordChannel discordChannel) {
		String discordSendMessageUrl = this.discordHelper.getServer() + "/api/v9/channels/" + discordChannel.getChannelId() + "/messages";
		String fileName = CharSequenceUtil.subAfter(finalFileName, "/", true);
		String paramsStr = this.messageParamsJson.replace("$content", content)
				.replace("$channel_id", discordChannel.getChannelId())
				.replace("$file_name", fileName)
				.replace("$final_file_name", finalFileName);
		ResponseEntity<String> responseEntity = postJson(discordSendMessageUrl, paramsStr, discordChannel);
		if (responseEntity.getStatusCode() != HttpStatus.OK) {
			log.error("发送图片消息到discord失败, status: {}, msg: {}", responseEntity.getStatusCodeValue(), responseEntity.getBody());
			return Message.of(ReturnCode.VALIDATION_ERROR, "发送图片消息到discord失败");
		}
		JSONObject result = new JSONObject(responseEntity.getBody());
		JSONArray attachments = result.optJSONArray("attachments");
		if (!attachments.isEmpty()) {
			return Message.success(attachments.getJSONObject(0).optString("url"));
		}
		return Message.failure("发送图片消息到discord失败: 图片不存在");
	}

	private void putFile(String uploadUrl, DataUrl dataUrl, DiscordChannel discordChannel) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("User-Agent", discordChannel.getUserAgent());
		headers.setContentType(MediaType.valueOf(dataUrl.getMimeType()));
		headers.setContentLength(dataUrl.getData().length);
		HttpEntity<byte[]> requestEntity = new HttpEntity<>(dataUrl.getData(), headers);
		new RestTemplate().put(uploadUrl, requestEntity);
	}

	private ResponseEntity<String> postJson(String paramsStr, DiscordChannel discordChannel) {
		return postJson(discordApiUrl, paramsStr, discordChannel);
	}

	private ResponseEntity<String> postJson(String url, String paramsStr, DiscordChannel discordChannel) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", discordChannel.getUserToken());
		headers.add("User-Agent", discordChannel.getUserAgent());
		HttpEntity<String> httpEntity = new HttpEntity<>(paramsStr, headers);
		return new RestTemplate().postForEntity(url, httpEntity, String.class);
	}

	private Message<Void> postJsonAndCheckStatus(String paramsStr, DiscordChannel discordChannel) {
		try {
			log.debug("postJson: {}", paramsStr);
			//String str ="{\"type\":5,\"application_id\":\"936929561302675456\",\"channel_id\":\"1133395395800731668\",\"guild_id\":\"1133215830201618533\",\"data\":{\"id\":\"1136957307595526194\",\"custom_id\":\"MJ::RemixModal::c11b36f2-5a80-4c8f-b4a3-91801e615cbb::4::1\",\"components\":[{\"type\":1,\"components\":[{\"type\":4,\"custom_id\":\"MJ::RemixModal::new_prompt\",\"value\":\"cat\"}]}]},\"session_id\":\"ebbeb54f1775c76b89ba7b39b469f054\",\"nonce\":\"1136957315283419136\"}";
			//JSONObject jsonObject = new JSONObject(str);
			ResponseEntity<String> responseEntity = postJson(paramsStr,discordChannel);
			log.debug("postJsonAndCheckStatus: {}", responseEntity);
			if (responseEntity.getStatusCode() == HttpStatus.NO_CONTENT) {
				return Message.success();
			}
			return Message.of(responseEntity.getStatusCodeValue(), CharSequenceUtil.sub(responseEntity.getBody(), 0, 100));
		} catch (HttpClientErrorException e) {
			try {
				JSONObject error = new JSONObject(e.getResponseBodyAsString());
				return Message.of(error.optInt("code", e.getRawStatusCode()), error.optString("message"));
			} catch (Exception je) {
				return Message.of(e.getRawStatusCode(), CharSequenceUtil.sub(e.getMessage(), 0, 100));
			}
		}
	}

}
