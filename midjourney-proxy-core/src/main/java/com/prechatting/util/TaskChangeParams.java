package com.prechatting.util;

import com.prechatting.enums.TaskAction;
import lombok.Data;

@Data
public class TaskChangeParams {
	private String id;
	private TaskAction action;
	private Integer index;
}
