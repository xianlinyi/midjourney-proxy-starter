package com.prechatting.dto;

import lombok.Data;

@Data
public class ImagineProgressDTO {
    /**
     * 任务id
     */
    private long taskId;

    /**
     * 提示词
     */
    private String prompt;

    /**
     * 进度
     */
    private int progress;

    /**
     * 任务状态 waitting:等待执行, running:执行中, success:执行成功, fail:执行失败
     */
    private String status;

    public static ImagineProgressDTO waitting(String prompt) {
        ImagineProgressDTO imagineProgressDTO = new ImagineProgressDTO();
        imagineProgressDTO.setProgress(0);
        imagineProgressDTO.setPrompt(prompt);
        imagineProgressDTO.setStatus("wait");
        return imagineProgressDTO;
    }
    public static ImagineProgressDTO running(String prompt, int progress) {
        ImagineProgressDTO imagineProgressDTO = new ImagineProgressDTO();
        imagineProgressDTO.setProgress(progress);
        imagineProgressDTO.setPrompt(prompt);
        imagineProgressDTO.setStatus("running");
        return imagineProgressDTO;
    }

    public static ImagineProgressDTO success(String prompt) {
        ImagineProgressDTO imagineProgressDTO = new ImagineProgressDTO();
        imagineProgressDTO.setProgress(100);
        imagineProgressDTO.setPrompt(prompt);
        imagineProgressDTO.setStatus("running");
        return imagineProgressDTO;
    }

    public ImagineProgressDTO task(long taskId) {
        this.taskId = taskId;
        return this;
    }

}
