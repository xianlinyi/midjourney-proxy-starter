package com.prechatting.service;


import com.prechatting.support.Task;
import com.prechatting.support.TaskCondition;

import java.util.List;

public interface TaskStoreService {

	void save(Task task);

	void delete(String id);

	Task get(String id);

	List<Task> list();

	List<Task> list(TaskCondition condition);

	Task findOne(TaskCondition condition);

}
