package com.prechatting.service.store;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.stream.StreamUtil;
import com.prechatting.enums.TaskStatus;
import com.prechatting.service.TaskStoreService;
import com.prechatting.support.Task;
import com.prechatting.support.TaskCondition;

import java.time.Duration;
import java.util.List;


public class InMemoryTaskStoreServiceImpl implements TaskStoreService {
	private final TimedCache<String, Task> taskMap;

	public InMemoryTaskStoreServiceImpl(Duration timeout) {
		this.taskMap = CacheUtil.newTimedCache(timeout.toMillis());
	}

	@Override
	public void save(Task task) {
		this.taskMap.put(task.getId(), task);
	}

	@Override
	public void delete(String key) {
		this.taskMap.remove(key);
	}

	@Override
	public Task get(String key) {
		return this.taskMap.get(key);
	}

	@Override
	public List<Task> list() {
		return ListUtil.toList(this.taskMap.iterator());
	}

	@Override
	public List<Task> list(TaskCondition condition) {
		return StreamUtil.of(this.taskMap.iterator()).filter(condition).toList();
	}

	@Override
	public Task findOne(TaskCondition condition) {
		return StreamUtil.of(this.taskMap.iterator()).filter(condition).filter(task -> task.getStatus().equals(TaskStatus.IN_PROGRESS) && task.getStatus().equals(TaskStatus.SUBMITTED)).findFirst().orElse(null);
	}

}
