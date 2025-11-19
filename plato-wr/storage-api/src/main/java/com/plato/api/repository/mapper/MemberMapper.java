package com.plato.api.repository.mapper;

import com.plato.api.repository.entity.MemberEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 群成员表Mapper
 * 
 * @author hc
 * @since 2025/11/19
 */
@Mapper
public interface MemberMapper {

        /**
         * 插入成员 (忽略重复)
         * 
         * @param entity 成员实体
         * @return 影响行数
         */
        @Insert("INSERT IGNORE INTO member (session_id, user_id) " +
                        "VALUES (#{sessionId}, #{userId})")
        int insert(MemberEntity entity);

        /**
         * 批量插入成员 (忽略重复)
         * 
         * @param entities 成员实体列表
         * @return 影响行数
         */
        @Insert("<script>" +
                        "INSERT IGNORE INTO member (session_id, user_id) VALUES " +
                        "<foreach collection='list' item='entity' separator=','>" +
                        "(#{entity.sessionId}, #{entity.userId})" +
                        "</foreach>" +
                        "</script>")
        int insertBatch(@Param("list") List<MemberEntity> entities);

        /**
         * 查询群成员ID列表
         * 
         * @param sessionId 会话ID
         * @return 成员ID列表
         */
        @Select("SELECT user_id FROM member WHERE session_id = #{sessionId}")
        List<Long> selectMemberIds(@Param("sessionId") Long sessionId);

        /**
         * 删除指定会话的所有成员
         * 
         * @param sessionId 会话ID
         * @return 影响行数
         */
        @Delete("DELETE FROM member WHERE session_id = #{sessionId}")
        int deleteBySessionId(@Param("sessionId") Long sessionId);

        /**
         * 批量删除成员
         * 
         * @param sessionId 会话ID
         * @param userIds   用户ID列表
         * @return 影响行数
         */
        @Delete("<script>" +
                        "DELETE FROM member WHERE session_id = #{sessionId} " +
                        "AND user_id IN " +
                        "<foreach collection='userIds' item='userId' open='(' separator=',' close=')'>" +
                        "#{userId}" +
                        "</foreach>" +
                        "</script>")
        int deleteBatch(@Param("sessionId") Long sessionId,
                        @Param("userIds") List<Long> userIds);
}
