package com.roomreservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.roomreservation.common.Constants;
import com.roomreservation.common.RuleKeys;
import com.roomreservation.entity.Room;
import com.roomreservation.entity.SysUser;
import com.roomreservation.entity.Watch;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.mapper.RoomMapper;
import com.roomreservation.mapper.WatchMapper;
import com.roomreservation.service.ActivityService;
import com.roomreservation.service.IRuleConfigService;
import com.roomreservation.service.ISysUserService;
import com.roomreservation.service.IWatchService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 关注 Service 实现
 */
@Service
/**
 * 关注业务实现：参数与权限校验、重复关注拦截、计分。
 */
public class WatchServiceImpl extends ServiceImpl<WatchMapper, Watch> implements IWatchService {

    @Resource
    private RoomMapper roomMapper;
    @Resource
    private ISysUserService sysUserService;
    @Resource
    private IRuleConfigService ruleConfigService;
    @Resource
    private ActivityService activityService;

    @Override
    public void addWatch(Watch watch) {
        if (watch.getUserId() == null || watch.getRoomId() == null || watch.getBookDate() == null
                || watch.getStartMin() == null || watch.getEndMin() == null) {
            throw new ServiceException(Constants.CODE_400, "关注参数不完整");
        }
        if (watch.getStartMin() >= watch.getEndMin()) {
            throw new ServiceException(Constants.CODE_400, "结束时间必须晚于开始时间");
        }
        Room room = roomMapper.selectById(watch.getRoomId());
        if (room == null) {
            throw new ServiceException(Constants.CODE_400, "琴房不存在");
        }
        SysUser user = sysUserService.getById(watch.getUserId());
        boolean admin = user != null && "admin".equals(user.getRole());
        boolean member = user != null && Boolean.TRUE.equals(user.getIsMember());
        if ("inner".equals(room.getRoomType()) && !admin && !member) {
            throw new ServiceException(Constants.CODE_403, "对内琴房仅会员可关注");
        }
        if (watch.getBookDate().isBefore(LocalDate.now())) {
            throw new ServiceException(Constants.CODE_400, "不能关注过去的日期");
        }
        int minUnit = ruleConfigService.getInt(RuleKeys.BOOKING_MIN_UNIT, 30);
        if (watch.getStartMin() < room.getOpenStart() || watch.getEndMin() > room.getOpenEnd()
                || watch.getStartMin() % minUnit != 0 || watch.getEndMin() % minUnit != 0) {
            throw new ServiceException(Constants.CODE_400, "关注时段需在可约窗内并按 " + minUnit + " 分钟对齐");
        }
        Long exists = count(new LambdaQueryWrapper<Watch>()
                .eq(Watch::getUserId, watch.getUserId())
                .eq(Watch::getRoomId, watch.getRoomId())
                .eq(Watch::getBookDate, watch.getBookDate())
                .eq(Watch::getStartMin, watch.getStartMin()));
        if (exists > 0) {
            throw new ServiceException(Constants.CODE_409, "你已关注该时段，无需重复关注");
        }
        Watch target = new Watch();
        target.setUserId(watch.getUserId());
        target.setRoomId(watch.getRoomId());
        target.setBookDate(watch.getBookDate());
        target.setStartMin(watch.getStartMin());
        target.setEndMin(watch.getEndMin());
        target.setStatus("active");
        save(target);
        activityService.record(target.getUserId(), "watch");
    }
}
