package com.plato.consumer.repository.mapper;

import com.plato.consumer.repository.entity.MemberEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 群成员表 Mapper
 * 
 * @author hc
 * @since 2025/11/20
 */
@Mapper
public interface MemberMapper {

    /**
     * 查询群成员ID列表
     * 
     * @param sessionId 会话ID
     * @return 成员ID列表
     */
    @Select("SELECT user_id FROM member WHERE session_id = #{sessionId}")
    List<Long> selectMemberIds(@Param("sessionId") Long sessionId);

    /**
     * 查询群成员列表（包含实体对象）
     * 
     * @param sessionId 会话ID
     * @return 成员实体列表
     */
    @Select("SELECT session_id, user_id FROM member WHERE session_id = #{sessionId}")
    List<MemberEntity> selectMembers(@Param("sessionId") Long sessionId);
}

