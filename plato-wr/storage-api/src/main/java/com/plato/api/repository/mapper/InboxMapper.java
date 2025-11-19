package com.plato.api.repository.mapper;

import com.plato.api.repository.entity.InboxEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 收件箱表Mapper
 * 
 * @author hc
 * @since 2025/11/19
 */
@Mapper
public interface InboxMapper {

        /**
         * 插入或更新收件箱记录
         * 
         * @param entity 收件箱实体
         * @return 影响行数
         */
        int insertOrUpdate(InboxEntity entity);

        /**
         * 批量插入或更新收件箱记录
         * 
         * @param entities 收件箱实体列表
         * @return 影响行数
         */
        int insertOrUpdateBatch(@Param("list") List<InboxEntity> entities);

        /**
         * 根据联合主键查询收件箱
         * 
         * @param userId    用户ID
         * @param sessionId 会话ID
         * @return 收件箱实体
         */
        @Select("SELECT * FROM inbox WHERE user_id = #{userId} AND session_id = #{sessionId}")
        InboxEntity selectByPrimaryKey(@Param("userId") Long userId, @Param("sessionId") Long sessionId);

        /**
         * 查询用户的所有收件箱
         * 
         * @param userId 用户ID
         * @return 收件箱列表
         */
        @Select("SELECT * FROM inbox WHERE user_id = #{userId} ORDER BY update_time DESC")
        List<InboxEntity> selectByUserId(@Param("userId") Long userId);

        /**
         * 更新已读位置
         * 
         * @param userId    用户ID
         * @param sessionId 会话ID
         * @param readSeqId 已读SeqID
         * @return 影响行数
         */
        @Update("UPDATE inbox SET last_read_seq_id = #{readSeqId} " +
                        "WHERE user_id = #{userId} AND session_id = #{sessionId}")
        int updateReadPosition(
                        @Param("userId") Long userId,
                        @Param("sessionId") Long sessionId,
                        @Param("readSeqId") Long readSeqId);

        /**
         * 更新置顶状态
         * 
         * @param userId    用户ID
         * @param sessionId 会话ID
         * @param isPinned  是否置顶
         * @return 影响行数
         */
        @Update("UPDATE inbox SET is_pinned = #{isPinned} " +
                        "WHERE user_id = #{userId} AND session_id = #{sessionId}")
        int updatePinned(
                        @Param("userId") Long userId,
                        @Param("sessionId") Long sessionId,
                        @Param("isPinned") Boolean isPinned);

        /**
         * 更新免打扰状态
         * 
         * @param userId    用户ID
         * @param sessionId 会话ID
         * @param isMuted   是否免打扰
         * @return 影响行数
         */
        @Update("UPDATE inbox SET is_muted = #{isMuted} " +
                        "WHERE user_id = #{userId} AND session_id = #{sessionId}")
        int updateMuted(
                        @Param("userId") Long userId,
                        @Param("sessionId") Long sessionId,
                        @Param("isMuted") Boolean isMuted);

        /**
         * 删除收件箱记录
         * 
         * @param userId    用户ID
         * @param sessionId 会话ID
         * @return 影响行数
         */
        @Delete("DELETE FROM inbox WHERE user_id = #{userId} AND session_id = #{sessionId}")
        int deleteByPrimaryKey(@Param("userId") Long userId, @Param("sessionId") Long sessionId);
}
