package com.roomreservation.config;

import cn.hutool.crypto.digest.BCrypt;
import com.roomreservation.common.RuleKeys;
import com.roomreservation.entity.Banner;
import com.roomreservation.entity.Instrument;
import com.roomreservation.entity.Notice;
import com.roomreservation.entity.Room;
import com.roomreservation.entity.RuleConfig;
import com.roomreservation.entity.SysUser;
import com.roomreservation.mapper.BannerMapper;
import com.roomreservation.mapper.InstrumentMapper;
import com.roomreservation.mapper.NoticeMapper;
import com.roomreservation.mapper.RoomMapper;
import com.roomreservation.mapper.RuleConfigMapper;
import com.roomreservation.service.ISysUserService;
import jakarta.annotation.Resource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 空库首启自动补种子数据：管理员与演示用户、5 间琴房与乐器、规则默认值、示例公告轮播
 */
@Component
public class DataInitializer implements ApplicationRunner {

    @Resource
    private ISysUserService sysUserService;
    @Resource
    private RoomMapper roomMapper;
    @Resource
    private InstrumentMapper instrumentMapper;
    @Resource
    private RuleConfigMapper ruleConfigMapper;
    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private BannerMapper bannerMapper;

    @Override
    public void run(ApplicationArguments args) {
        initUsers();
        initRooms();
        initRules();
        initContents();
    }

    private void initUsers() {
        if (sysUserService.count() > 0) {
            return;
        }
        List<SysUser> users = new ArrayList<>();
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(BCrypt.hashpw("admin123"));
        admin.setName("系统管理员");
        admin.setStudentNo("admin");
        admin.setEmail("admin@room.local");
        admin.setRole("admin");
        admin.setIsMember(false);
        admin.setCredit(100);
        admin.setStatus("normal");
        users.add(admin);

        SysUser demo = new SysUser();
        demo.setUsername("demo");
        demo.setPassword(BCrypt.hashpw("123456"));
        demo.setName("演示用户");
        demo.setStudentNo("2023000000");
        demo.setEmail("demo@room.local");
        demo.setRole("user");
        demo.setIsMember(false);
        demo.setCredit(100);
        demo.setStatus("normal");
        users.add(demo);
        sysUserService.saveBatch(users);
    }

    private void initRooms() {
        if (roomMapper.selectCount(null) > 0) {
            return;
        }
        // 对内 2 间，对外 3 间，可约窗 6 点至 22 点
        int[][] specs = {
                {1, 360, 1320}, {1, 360, 1320},
                {0, 360, 1320}, {0, 360, 1320}, {0, 360, 1320}
        };
        String[] names = {"对内琴房 A201", "对内琴房 A202", "对外琴房 B101", "对外琴房 B102", "对外琴房 B103"};
        // 每间琴房乐器，名称、数量、备注
        String[][][] insts = {
                {{"钢琴", "1", "立式钢琴"}, {"小提琴", "2", "含备用琴弦"}},
                {{"钢琴", "1", "三角钢琴"}, {"架子鼓", "1", "需自备鼓棒"}},
                {{"钢琴", "2", "立式钢琴"}, {"电子琴", "1", "含电源"}},
                {{"古筝", "3", "含指甲"}, {"扬琴", "1", "含琴竹"}},
                {{"吉他", "4", "民谣吉他"}, {"尤克里里", "3", "入门款"}}
        };
        String[] notes = {
                "仅供会员预约，室内禁止饮食",
                "仅供会员预约，鼓组使用需自备鼓棒",
                "室内禁止饮食，保持安静",
                "室内禁止饮食，乐器轻拿轻放",
                "室内禁止饮食，吉他需调弦使用"
        };
        for (int i = 0; i < names.length; i++) {
            Room room = new Room();
            room.setName(names[i]);
            room.setRoomType(specs[i][0] == 1 ? "inner" : "outer");
            room.setOpenStart(specs[i][1]);
            room.setOpenEnd(specs[i][2]);
            room.setDescription(notes[i]);
            room.setStatus("normal");
            roomMapper.insert(room);
            for (String[] instSpec : insts[i]) {
                Instrument inst = new Instrument();
                inst.setRoomId(room.getId());
                inst.setName(instSpec[0]);
                inst.setCount(Integer.valueOf(instSpec[1]));
                inst.setNote(instSpec[2]);
                instrumentMapper.insert(inst);
            }
        }
    }

    private void initRules() {
        if (ruleConfigMapper.selectCount(null) > 0) {
            return;
        }
        String[][] defaults = {
                {RuleKeys.BOOKING_MIN_UNIT, "30", "最小可约单位，分钟"},
                {RuleKeys.BOOKING_MAX_DURATION, "240", "单次预约最大时长，分钟"},
                {RuleKeys.BOOKING_ADVANCE_DAYS, "7", "提前预约天数"},
                {RuleKeys.BOOKING_WEEKLY_LIMIT, "3", "每周最大预约次数"},
                {RuleKeys.BOOKING_WEEKLY_CANCEL_LIMIT, "2", "每周最大退约次数"},
                {RuleKeys.CREDIT_INITIAL, "100", "信用初始值"},
                {RuleKeys.CREDIT_MAX, "100", "信用上限"},
                {RuleKeys.CREDIT_LOW, "60", "信用暂停阈值，低于该值暂停预约"},
                {RuleKeys.CREDIT_DAILY_RESTORE, "1", "信用每日自然恢复值"}
        };
        for (String[] d : defaults) {
            RuleConfig cfg = new RuleConfig();
            cfg.setRuleKey(d[0]);
            cfg.setRuleValue(d[1]);
            cfg.setNote(d[2]);
            ruleConfigMapper.insert(cfg);
        }
    }

    private void initContents() {
        if (noticeMapper.selectCount(null) == 0) {
            Notice notice = new Notice();
            notice.setTitle("琴房预约系统上线公告");
            notice.setContent("系统正式启用，会员可预约对内琴房，普通用户可预约对外琴房。预约前请阅读琴房注意事项。");
            notice.setStatus("normal");
            noticeMapper.insert(notice);
        }
        if (bannerMapper.selectCount(null) == 0) {
            Banner banner = new Banner();
            banner.setImage("/img/banner-default.png");
            banner.setLink("");
            banner.setSort(1);
            banner.setStatus("normal");
            bannerMapper.insert(banner);
        }
    }
}
