package com.prechatting.service.listener;

import com.prechatting.dto.ImagineProgressDTO;
import com.prechatting.support.Task;

/**
 * 任务监听器
 */
public interface IProgressListener {
    /**
     * 任务开始
     * @param task
     */
    void onStart(Task task);

    /**
     * 任务进度变动
     * @param task
     */
    void onProgress(Task task);

    /**
     * 任务成功
     * @param task
     */
    void onSuccess(Task task);

    /**
     * 任务失败
     * @param task
     */
    void onFail(Task task);
}
