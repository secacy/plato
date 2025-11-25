package com.plato.storage.repository.mapper;

import com.plato.storage.repository.entity.SessionMetaEntity;
import org.apache.ibatis.annotations.*;

/**
 * 会话元数据 Mapper
 * 
 * @author hc
 * @since 2025/11/20
 */
@Mapper
public interface SessionMetaMapper {

        /**
         * 查询会话元数据（加锁）
         * 用于事务内获取并锁定 max_seq
         * 
         * @param sessionId 会话ID
         * @return 会话元数据
         */
        @Select("SELECT * FROM session_meta WHERE session_id = #{sessionId} FOR UPDATE")
        @Results(id = "sessionMetaResult", value = {
                        @Result(column = "session_id", property = "sessionId"),
                        @Result(column = "max_seq_id", property = "maxSeq"),
                        @Result(column = "last_msg_time", property = "lastMsgTime"),
                        @Result(column = "last_msg_sender_id", property = "lastMsgSenderId"),
                        @Result(column = "last_msg_type", property = "lastMsgType"),
                        @Result(column = "last_msg_content", property = "lastMsgContent"),
                        @Result(column = "create_time", property = "createTime"),
                        @Result(column = "update_time", property = "updateTime")
        })
        SessionMetaEntity selectForUpdate(@Param("sessionId") Long sessionId);

        /**
         * 插入会话元数据（如果不存在）
         * 
         * @param entity 会话元数据
         * @return 影响行数
         */
        @Insert("INSERT INTO session_meta (session_id, max_seq_id, last_msg_time, last_msg_sender_id, " +
                        "last_msg_type, last_msg_content, create_time, update_time) " +
                        "VALUES (#{sessionId}, #{maxSeq}, #{lastMsgTime}, #{lastMsgSenderId}, #{lastMsgType}, " +
                        "#{lastMsgContent}, #{createTime}, #{updateTime}) " +
                        "ON DUPLICATE KEY UPDATE session_id = session_id")
        int insertIfNotExists(SessionMetaEntity entity);

        /**
         * 更新会话的 max_seq
         * 
         * @param sessionId 会话ID
         * @param newMaxSeq 新的最大序列号
         * @return 影响行数
         */
        @Update("UPDATE session_meta SET max_seq_id = #{newMaxSeq}, update_time = NOW() " +
                        "WHERE session_id = #{sessionId}")
        int updateMaxSeq(@Param("sessionId") Long sessionId, @Param("newMaxSeq") Long newMaxSeq);

        /**
         * 查询会话元数据（不加锁）
         * 
         * @param sessionId 会话ID
         * @return 会话元数据
         */
        @Select("SELECT * FROM session_meta WHERE session_id = #{sessionId}")
        @ResultMap("sessionMetaResult")
        SessionMetaEntity selectById(@Param("sessionId") Long sessionId);

        /**
         * 更新会话快照（消息预览信息）
         * 使用 XML 配置，见 SessionMetaMapper.xml
         * 
         * @param entity 会话元数据（包含快照字段）
         * @return 影响行数
         */
        int updateSnapshot(SessionMetaEntity entity);
}
