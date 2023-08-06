package com.prechatting.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.prechatting.enums.TaskAction;
import com.prechatting.enums.TaskStatus;
import com.prechatting.service.listener.DefaultProgressListener;
import com.prechatting.service.listener.IProgressListener;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Data
@ApiModel("任务")
public class Task implements Serializable {
	@Serial
	private static final long serialVersionUID = -674915748204390789L;

	private TaskAction action;
	@ApiModelProperty("任务ID")
	private String id;
	@ApiModelProperty("提示词")
	private String prompt;
	@ApiModelProperty("提示词-英文")
	private String promptEn;
	@ApiModelProperty("remix的提示词")
	private String remixPrompt;

	@ApiModelProperty("任务所属的会话")
	private DiscordChannel discordChannel;

	@ApiModelProperty("任务描述")
	private String description;
	@ApiModelProperty("自定义参数")
	private String state;
	@ApiModelProperty("提交时间")
	private Long submitTime;
	@ApiModelProperty("开始执行时间")
	private Long startTime;
	@ApiModelProperty("结束时间")
	private Long finishTime;
	@ApiModelProperty("图片url")
	private String imageUrl;
	@ApiModelProperty("任务状态")
	private TaskStatus status = TaskStatus.NOT_START;
	@ApiModelProperty("任务进度")
	private String progress;
	@ApiModelProperty("失败原因")
	private String failReason;

	/**
	 * discord的一些参数
	 */
	// 临时标识
	private String nonce;
	// 操作组件Id
	private String componentId;
	private Integer flags;
	private String messageId;
	private String progressMessageId;
	private String messageHash;


	// 任务扩展属性，仅支持基本类型
	private Map<String, Object> properties;
	@JsonIgnore
	private IProgressListener listener =new DefaultProgressListener();



	@JsonIgnore
	private final transient Object lock = new Object();


	public synchronized void setComponentId(String componentId) {
		this.componentId = componentId;
		notify();
	}

	public void setProgress(String progress) {
		this.progress = progress;
		listener.onProgress(this);
	}

	public void sleep() throws InterruptedException {
		synchronized (this.lock) {
			this.lock.wait();
		}
	}

	public void awake() {
		synchronized (this.lock) {
			this.lock.notifyAll();
		}
	}

	public void start() {
		this.startTime = System.currentTimeMillis();
		this.status = TaskStatus.SUBMITTED;
		this.progress = "0%";
		listener.onStart(this);
	}

	public void success() {
		this.finishTime = System.currentTimeMillis();
		this.status = TaskStatus.SUCCESS;
		this.progress = "100%";
		listener.onSuccess(this);
	}

	public void fail(String reason) {
		this.finishTime = System.currentTimeMillis();
		this.status = TaskStatus.FAILURE;
		this.failReason = reason;
		this.progress = "";
		listener.onFail(this);
	}

	public Task setProperty(String name, Object value) {
		getProperties().put(name, value);
		return this;
	}

	public Task removeProperty(String name) {
		getProperties().remove(name);
		return this;
	}

	public Object getProperty(String name) {
		return getProperties().get(name);
	}

	@SuppressWarnings("unchecked")
	public <T> T getPropertyGeneric(String name) {
		return (T) getProperty(name);
	}

	public <T> T getProperty(String name, Class<T> clz) {
		return clz.cast(getProperty(name));
	}

	public Map<String, Object> getProperties() {
		if (this.properties == null) {
			this.properties = new HashMap<>();
		}
		return this.properties;
	}
}
