package com.roomreservation.service.admin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.roomreservation.common.Constants;
import com.roomreservation.entity.Booking;
import com.roomreservation.entity.Instrument;
import com.roomreservation.entity.Room;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.mapper.BookingMapper;
import com.roomreservation.mapper.InstrumentMapper;
import com.roomreservation.mapper.RoomMapper;
import com.roomreservation.service.CacheService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端琴房 Service：琴房增删改查与乐器明细整体覆盖
 */
@Service
public class AdminRoomService {
    @Resource
    private CacheService cacheService;


    @Resource
    private RoomMapper roomMapper;
    @Resource
    private InstrumentMapper instrumentMapper;
    @Resource
    private BookingMapper bookingMapper;

    /**
     * 全量琴房列表（含对内琴房），每行携带该琴房乐器明细
     */
    public List<Map<String, Object>> listAll() {
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>().orderByAsc(Room::getId));
        if (rooms.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Integer, List<Instrument>> byRoom = new HashMap<>();
        for (Instrument inst : instrumentMapper.selectList(new LambdaQueryWrapper<Instrument>().orderByAsc(Instrument::getId))) {
            byRoom.computeIfAbsent(inst.getRoomId(), k -> new ArrayList<>()).add(inst);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Room room : rooms) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", room.getId());
            row.put("name", room.getName());
            row.put("roomType", room.getRoomType());
            row.put("openStart", room.getOpenStart());
            row.put("openEnd", room.getOpenEnd());
            row.put("description", room.getDescription());
            row.put("status", room.getStatus());
            row.put("instruments", byRoom.getOrDefault(room.getId(), new ArrayList<>()));
            rows.add(row);
        }
        return rows;
    }

    /**
     * 新增琴房，instruments 可空
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(Map<String, Object> body) {
        evictRoomCache();
        Room room = buildRoom(body, true);
        roomMapper.insert(room);
        replaceInstruments(room.getId(), body.get("instruments"));
        Map<String, Object> data = new HashMap<>();
        data.put("id", room.getId());
        return data;
    }

    /**
     * 修改琴房，仅更新传入字段；传 instruments 时整体覆盖乐器明细
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Integer id, Map<String, Object> body) {
        evictRoomCache();
        Room room = requireRoom(id);
        LambdaUpdateWrapper<Room> wrapper = new LambdaUpdateWrapper<Room>().eq(Room::getId, id);
        if (body.containsKey("name")) {
            String name = body.get("name") == null ? null : body.get("name").toString().trim();
            if (StrUtil.isBlank(name)) {
                throw new ServiceException(Constants.CODE_400, "琴房名称不能为空");
            }
            wrapper.set(Room::getName, name);
        }
        if (body.containsKey("roomType")) {
            String roomType = body.get("roomType") == null ? null : body.get("roomType").toString();
            checkRoomType(roomType);
            wrapper.set(Room::getRoomType, roomType);
        }
        Integer openStart = parseInt(body.get("openStart"), "openStart");
        Integer openEnd = parseInt(body.get("openEnd"), "openEnd");
        boolean hasStart = body.containsKey("openStart");
        boolean hasEnd = body.containsKey("openEnd");
        if (hasStart || hasEnd) {
            int baseStart = hasStart ? openStart : room.getOpenStart();
            int baseEnd = hasEnd ? openEnd : room.getOpenEnd();
            checkOpenWindow(baseStart, baseEnd);
            if (hasStart) {
                wrapper.set(Room::getOpenStart, openStart);
            }
            if (hasEnd) {
                wrapper.set(Room::getOpenEnd, openEnd);
            }
        }
        if (body.containsKey("description")) {
            String description = body.get("description") == null ? null : body.get("description").toString();
            wrapper.set(Room::getDescription, StrUtil.trim(description));
        }
        if (body.containsKey("status")) {
            String status = body.get("status") == null ? null : body.get("status").toString();
            if (StrUtil.isBlank(status)) {
                throw new ServiceException(Constants.CODE_400, "状态不能为空");
            }
            wrapper.set(Room::getStatus, status);
        }
        if (wrapper.getSqlSet().isEmpty()) {
            throw new ServiceException(Constants.CODE_400, "没有可更新的字段");
        }
        roomMapper.update(null, wrapper);
        if (body.containsKey("instruments")) {
            replaceInstruments(id, body.get("instruments"));
        }
    }

    /**
     * 删除琴房，存在预约或乐器记录时返回 409
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        evictRoomCache();
        requireRoom(id);
        if (bookingMapper.selectCount(new LambdaQueryWrapper<Booking>().eq(Booking::getRoomId, id)) > 0) {
            throw new ServiceException(Constants.CODE_409, "该琴房存在预约记录，不能删除");
        }
        if (instrumentMapper.selectCount(new LambdaQueryWrapper<Instrument>().eq(Instrument::getRoomId, id)) > 0) {
            throw new ServiceException(Constants.CODE_409, "该琴房存在乐器记录，请先清空乐器明细");
        }
        roomMapper.deleteById(id);
    }

    private Room buildRoom(Map<String, Object> body, boolean requireAll) {
        String name = body.get("name") == null ? null : body.get("name").toString().trim();
        String roomType = body.get("roomType") == null ? null : body.get("roomType").toString();
        Integer openStart = parseInt(body.get("openStart"), "openStart");
        Integer openEnd = parseInt(body.get("openEnd"), "openEnd");
        if (requireAll && (StrUtil.isBlank(name) || StrUtil.isBlank(roomType) || openStart == null || openEnd == null)) {
            throw new ServiceException(Constants.CODE_400, "琴房名称、类型与可约时间窗不能为空");
        }
        if (StrUtil.isBlank(name)) {
            throw new ServiceException(Constants.CODE_400, "琴房名称不能为空");
        }
        checkRoomType(roomType);
        checkOpenWindow(openStart, openEnd);
        Room room = new Room();
        room.setName(name);
        room.setRoomType(roomType);
        room.setOpenStart(openStart);
        room.setOpenEnd(openEnd);
        Object description = body.get("description");
        room.setDescription(description == null ? null : description.toString().trim());
        room.setStatus("normal");
        return room;
    }

    private void checkRoomType(String roomType) {
        if (!"inner".equals(roomType) && !"outer".equals(roomType)) {
            throw new ServiceException(Constants.CODE_400, "琴房类型只能为 inner 或 outer");
        }
    }

    private void checkOpenWindow(Integer openStart, Integer openEnd) {
        if (openStart == null || openEnd == null || openStart < 0 || openEnd > 1440 || openStart >= openEnd) {
            throw new ServiceException(Constants.CODE_400, "可约时间窗应为 0 至 1440 分钟且开始早于结束");
        }
    }

    /**
     * 乐器明细整体覆盖：先删后插
     */
    private void replaceInstruments(Integer roomId, Object instrumentsObj) {
        instrumentMapper.delete(new LambdaQueryWrapper<Instrument>().eq(Instrument::getRoomId, roomId));
        if (instrumentsObj == null) {
            return;
        }
        if (!(instrumentsObj instanceof List<?> list)) {
            throw new ServiceException(Constants.CODE_400, "instruments 应为数组");
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                throw new ServiceException(Constants.CODE_400, "乐器项格式不正确");
            }
            String name = map.get("name") == null ? null : map.get("name").toString().trim();
            if (StrUtil.isBlank(name)) {
                throw new ServiceException(Constants.CODE_400, "乐器名称不能为空");
            }
            Integer count = map.containsKey("count") ? parseInt(map.get("count"), "乐器数量") : 1;
            if (count == null || count < 1) {
                throw new ServiceException(Constants.CODE_400, "乐器数量至少为 1");
            }
            Object note = map.get("note");
            Instrument inst = new Instrument();
            inst.setRoomId(roomId);
            inst.setName(name);
            inst.setCount(count);
            inst.setNote(note == null ? null : note.toString().trim());
            instrumentMapper.insert(inst);
        }
    }

    private Room requireRoom(Integer id) {
        Room room = id == null ? null : roomMapper.selectById(id);
        if (room == null) {
            throw new ServiceException(Constants.CODE_404, "琴房不存在");
        }
        return room;
    }

    private Integer parseInt(Object value, String field) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new ServiceException(Constants.CODE_400, field + " 应为整数");
        }
    }

    private void evictRoomCache() {
        cacheService.evictByPrefix("rooms:");
        cacheService.evictByPrefix("room:");
        cacheService.evictByPrefix("free:");
    }
}
