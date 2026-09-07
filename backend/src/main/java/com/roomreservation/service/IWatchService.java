package com.roomreservation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.roomreservation.entity.Watch;

/**
 * 空出提醒关注 Service
 */
public interface IWatchService extends IService<Watch> {

    /**
     * 新增关注，同一用户对同一琴房日期起点只关注一次
     */
    void addWatch(Watch watch);
}
