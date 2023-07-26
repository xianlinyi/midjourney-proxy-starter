package com.prechatting.support;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public abstract class TaskMixin {
	@JsonIgnore
	private Map<String, Object> properties;
}
