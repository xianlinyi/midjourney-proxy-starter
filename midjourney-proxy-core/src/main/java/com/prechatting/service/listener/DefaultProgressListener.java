package com.prechatting.service.listener;

import com.prechatting.support.Task;

public class DefaultProgressListener implements IProgressListener{
    //do nothing

    /**
     * 任务开始
     * @param task
     */
    @Override
    public void onStart(Task task) {

    }

    /**
     * 任务进度变动
     * @param task
     */
    @Override
    public void onProgress(Task task) {
    }

    /**
     * 任务成功
     * @param task
     */
    @Override
    public void onSuccess(Task task) {

    }

    /**
     * 任务失败
     * @param task
     */
    @Override
    public void onFail(Task task) {

    }
}
