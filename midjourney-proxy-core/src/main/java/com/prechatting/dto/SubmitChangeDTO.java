package com.prechatting.dto;

import com.prechatting.ProxyProperties;
import com.prechatting.enums.TaskAction;
import com.prechatting.support.Task;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Message;

import java.util.Map;


@Data
@ApiModel("变化任务提交参数")
@EqualsAndHashCode(callSuper = true)
public class SubmitChangeDTO extends BaseSubmitDTO {

	@ApiModelProperty(value = "任务ID", required = true, example = "\"1320098173412546\"")
	private String taskId;

	@ApiModelProperty(value = "任务信息 task和taskId只传一个", required = true)
	private Task task;

	@ApiModelProperty(value = "remix的提示次")
	private String remixPrompt;

	@ApiModelProperty(value = "UPSCALE(放大); VARIATION(变换); REROLL(重新生成)", required = true,
			allowableValues = "UPSCALE, VARIATION, REROLL", example = "UPSCALE")
	private TaskAction action;

	@ApiModelProperty(value = "序号(1~4), action为UPSCALE,VARIATION时必传", allowableValues = "range[1, 4]", example = "1")
	private Integer index;


}
