package com.prechatting.service.listener;

import com.prechatting.dto.ImagineProgressDTO;
import com.prechatting.support.Task;

public interface IProgressListener {
    void onStart(Task task);
    void onProgress(Task task);

    void onSuccess(Task task);

    void onFail(Task task);
}
