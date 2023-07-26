package com.prechatting.dto;

import com.prechatting.ProxyProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@ApiModel("Imagine提交参数")
@EqualsAndHashCode(callSuper = true)
public class SubmitImagineDTO extends BaseSubmitDTO {

	@ApiModelProperty(value = "提示词", required = true, example = "Cat")
	private String prompt;

	@ApiModelProperty(value = "垫图base64")
	private String base64;

	@ApiModelProperty(value = "discord配置")
	private ProxyProperties.DiscordConfig discordConfig;

}
