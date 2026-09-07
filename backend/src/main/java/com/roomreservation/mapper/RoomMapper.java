package com.roomreservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.roomreservation.entity.Room;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 琴房 Mapper
 */
public interface RoomMapper extends BaseMapper<Room> {

    /**
     * 行锁琴房，用于预约事务内串行化同房写操作
     */
    @Select("SELECT id FROM room WHERE id = #{roomId} FOR UPDATE")
    Integer lockRoom(@Param("roomId") Integer roomId);
}
