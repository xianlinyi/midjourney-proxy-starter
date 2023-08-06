package com.prechatting.service;

import com.prechatting.dto.*;
import com.prechatting.result.SubmitResultVO;
import com.prechatting.service.listener.IProgressListener;
import com.prechatting.support.Task;

import java.util.List;

public interface MJService {

    /**
     * 提交Imagine任务
     * @param imagineDTO
     * @return
     */
    SubmitResultVO imagine(SubmitImagineDTO imagineDTO);

    SubmitResultVO imagine(SubmitImagineDTO imagineDTO, IProgressListener progressListener);

    /**
     * 绘图变化-simple
     * @param simpleChangeDTO
     * @return
     */
    SubmitResultVO simpleChange(SubmitSimpleChangeDTO simpleChangeDTO);

    /**
     * 绘图变化
     * @param changeDTO
     * @return
     */
    SubmitResultVO change(SubmitChangeDTO changeDTO);

    SubmitResultVO change(SubmitChangeDTO changeDTO, IProgressListener progressListener);

    /**
     * 提交Describe任务
     * @param describeDTO
     * @return
     */
    SubmitResultVO describe(SubmitDescribeDTO describeDTO);

    /**
     * 提交Blend任务
     * @param blendDTO
     * @return
     */
    SubmitResultVO blend(SubmitBlendDTO blendDTO);

    /**
     * 查询所有任务
     * @return
     */
    List<Task> list();

    /**
     * 指定ID获取任务
     * @param id
     * @return
     */
    Task fetch(String id);

    /**
     * 查询任务队列
     * @return
     */
    List<Task> queue();

    /**
     * 根据条件查询任务
     * @param conditionDTO
     * @return
     */
    List<Task> listByCondition(TaskConditionDTO conditionDTO);
}
