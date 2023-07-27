package com.prechatting.controller;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.RandomUtil;
import com.prechatting.Constants;
import com.prechatting.ProxyProperties;
import com.prechatting.ReturnCode;
import com.prechatting.dto.BaseSubmitDTO;
import com.prechatting.dto.SubmitBlendDTO;
import com.prechatting.dto.SubmitChangeDTO;
import com.prechatting.dto.SubmitDescribeDTO;
import com.prechatting.dto.SubmitImagineDTO;
import com.prechatting.dto.SubmitSimpleChangeDTO;
import com.prechatting.enums.TaskAction;
import com.prechatting.enums.TaskStatus;
import com.prechatting.result.SubmitResultVO;
import com.prechatting.service.*;
import com.prechatting.support.Task;
import com.prechatting.support.TaskCondition;
import com.prechatting.util.BannedPromptUtils;
import com.prechatting.util.ConvertUtils;
import com.prechatting.util.MimeTypeUtils;
import com.prechatting.util.TaskChangeParams;
import eu.maxschuster.dataurl.DataUrl;
import eu.maxschuster.dataurl.DataUrlSerializer;
import eu.maxschuster.dataurl.IDataUrlSerializer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Api(tags = "任务提交")
@RestController
@RequestMapping("/submit")
@RequiredArgsConstructor
public class SubmitController {
	private final MJService mjService;

	@ApiOperation(value = "提交Imagine任务")
	@PostMapping("/imagine")
	public SubmitResultVO imagine(@RequestBody SubmitImagineDTO imagineDTO) {
		return mjService.imagine(imagineDTO);
	}

	@ApiOperation(value = "绘图变化-simple")
	@PostMapping("/simple-change")
	public SubmitResultVO simpleChange(@RequestBody SubmitSimpleChangeDTO simpleChangeDTO) {
		return mjService.simpleChange(simpleChangeDTO);
	}

	@ApiOperation(value = "绘图变化")
	@PostMapping("/change")
	public SubmitResultVO change(@RequestBody SubmitChangeDTO changeDTO) {
		return mjService.change(changeDTO);
	}

	@ApiOperation(value = "提交Describe任务")
	@PostMapping("/describe")
	public SubmitResultVO describe(@RequestBody SubmitDescribeDTO describeDTO) {
		return mjService.describe(describeDTO);
	}

	@ApiOperation(value = "提交Blend任务")
	@PostMapping("/blend")
	public SubmitResultVO blend(@RequestBody SubmitBlendDTO blendDTO) {
		return mjService.blend(blendDTO);
	}
}
