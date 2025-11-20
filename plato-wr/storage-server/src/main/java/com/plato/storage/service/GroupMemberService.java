package com.plato.storage.service;

import com.plato.storage.event.GroupMemberEvent;
import com.plato.storage.producer.StorageEventProducer;
import com.plato.storage.redis.GroupMemberRedisService;
import com.plato.storage.repository.entity.MemberEntity;
import com.plato.storage.repository.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 群成员业务服务
 * 
 * 职责：管理群成员关系（同步事务性操作）
 * 
 * @author hc
 * @since 2025/11/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupMemberService {

    private final MemberMapper memberMapper;
    private final GroupMemberRedisService groupMemberRedisService;
    private final StorageEventProducer eventProducer;

    /**
     * 同步群成员列表
     * 
     * 场景：建群、拉人（由业务层调用）
     * 
     * @param sessionId 会话ID
     * @param memberIds 成员ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void upsertGroupMembers(Long sessionId, List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return;
        }

        // 1. 写入TiDB（事务）
        List<MemberEntity> entities = memberIds.stream()
                .map(memberId -> MemberEntity.builder()
                        .sessionId(sessionId)
                        .userId(memberId)
                        .build())
                .collect(Collectors.toList());

        // 使用批量插入（INSERT IGNORE）
        memberMapper.insertBatch(entities);

        // 2. 更新Redis缓存
        groupMemberRedisService.addMembers(sessionId, memberIds);

        // 3. 发送事件（兜底，供Consumer确保一致性）
        GroupMemberEvent event = GroupMemberEvent.builder()
                .eventType(GroupMemberEvent.EventType.PATCH)
                .sessionId(sessionId)
                .addedMemberIds(memberIds)
                .timestamp(System.currentTimeMillis())
                .build();

        eventProducer.sendGroupMemberEvent(event);

        log.info("Group members upserted, sessionId={}, count={}", sessionId, memberIds.size());
    }

    /**
     * 移除群成员
     * 
     * 场景：踢人、退群
     * 
     * @param sessionId 会话ID
     * @param memberIds 成员ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeGroupMembers(Long sessionId, List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return;
        }

        // 1. 从TiDB删除（事务）
        memberMapper.deleteBatch(sessionId, memberIds);

        // 2. 从Redis删除
        groupMemberRedisService.removeMembers(sessionId, memberIds);

        // 3. 发送事件（兜底）
        GroupMemberEvent event = GroupMemberEvent.builder()
                .eventType(GroupMemberEvent.EventType.PATCH)
                .sessionId(sessionId)
                .removedMemberIds(memberIds)
                .timestamp(System.currentTimeMillis())
                .build();

        eventProducer.sendGroupMemberEvent(event);

        log.info("Group members removed, sessionId={}, count={}", sessionId, memberIds.size());
    }

    /**
     * 重置群成员（全量同步）
     * 
     * 场景：数据修复、夜间校对
     * 
     * @param sessionId 会话ID
     * @param memberIds 完整的成员ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetGroupMembers(Long sessionId, List<Long> memberIds) {
        // 1. 删除TiDB中的所有旧成员
        memberMapper.deleteBySessionId(sessionId);

        // 2. 写入新成员列表
        if (!memberIds.isEmpty()) {
            List<MemberEntity> entities = memberIds.stream()
                    .map(memberId -> MemberEntity.builder()
                            .sessionId(sessionId)
                            .userId(memberId)
                            .build())
                    .collect(Collectors.toList());

            // 使用批量插入（INSERT IGNORE）
            memberMapper.insertBatch(entities);
        }

        // 3. 重置Redis缓存
        groupMemberRedisService.cacheMembers(sessionId, memberIds);

        // 4. 发送事件（兜底）
        GroupMemberEvent event = GroupMemberEvent.builder()
                .eventType(GroupMemberEvent.EventType.RESET)
                .sessionId(sessionId)
                .allMemberIds(memberIds)
                .timestamp(System.currentTimeMillis())
                .build();

        eventProducer.sendGroupMemberEvent(event);

        log.info("Group members reset, sessionId={}, count={}", sessionId, memberIds.size());
    }

    /**
     * 获取群成员列表
     * 
     * @param sessionId 会话ID
     * @return 成员ID列表
     */
    public List<Long> getGroupMembers(Long sessionId) {
        // 1. 先查Redis缓存
        List<Long> members = groupMemberRedisService.getMembers(sessionId);

        if (members != null) {
            return members;
        }

        // 2. 缓存未命中，查TiDB
        members = memberMapper.selectMemberIds(sessionId);

        // 3. 回写缓存
        if (!members.isEmpty()) {
            groupMemberRedisService.cacheMembers(sessionId, members);
        }

        return members;
    }
}
