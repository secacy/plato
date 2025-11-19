package com.plato.api.repository.mapper;

import com.plato.api.repository.entity.MessageEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 消息表Mapper
 * 
 * @author hc
 * @since 2025/11/19
 */
@Mapper
public interface MessageMapper {

        /**
         * 插入消息
         * 
         * @param entity 消息实体
         * @return 影响行数
         */
        int insert(MessageEntity entity);

        /**
         * 根据联合主键查询消息
         * 
         * @param sessionId 会话ID
         * @param seqId     序列ID
         * @return 消息实体
         */
        @Select("SELECT * FROM message WHERE session_id = #{sessionId} AND seq_id = #{seqId}")
        MessageEntity selectByPrimaryKey(@Param("sessionId") Long sessionId, @Param("seqId") Long seqId);

        /**
         * 拉取消息历史 - 向前翻页 (BACKWARD)
         * 
         * @param sessionId   会话ID
         * @param anchorSeqId 锚点SeqID
         * @param limit       限制条数
         * @return 消息列表
         */
        @Select("SELECT * FROM message " +
                        "WHERE session_id = #{sessionId} AND seq_id < #{anchorSeqId} " +
                        "ORDER BY seq_id DESC LIMIT #{limit}")
        List<MessageEntity> selectBackward(
                        @Param("sessionId") Long sessionId,
                        @Param("anchorSeqId") Long anchorSeqId,
                        @Param("limit") int limit);

        /**
         * 拉取消息历史 - 向后翻页 (FORWARD)
         * 
         * @param sessionId   会话ID
         * @param anchorSeqId 锚点SeqID
         * @param limit       限制条数
         * @return 消息列表
         */
        @Select("SELECT * FROM message " +
                        "WHERE session_id = #{sessionId} AND seq_id > #{anchorSeqId} " +
                        "ORDER BY seq_id ASC LIMIT #{limit}")
        List<MessageEntity> selectForward(
                        @Param("sessionId") Long sessionId,
                        @Param("anchorSeqId") Long anchorSeqId,
                        @Param("limit") int limit);

        /**
         * 更新消息状态
         * 
         * @param sessionId 会话ID
         * @param seqId     序列ID
         * @param newStatus 新状态
         * @return 影响行数
         */
        @Update("UPDATE message SET status = #{newStatus} " +
                        "WHERE session_id = #{sessionId} AND seq_id = #{seqId}")
        int updateStatus(
                        @Param("sessionId") Long sessionId,
                        @Param("seqId") Long seqId,
                        @Param("newStatus") Integer newStatus);
}
