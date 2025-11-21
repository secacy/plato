package com.plato.consumer.service;

import com.plato.consumer.elasticsearch.MessageDocument;
import com.plato.consumer.elasticsearch.MessageSearchRepository;
import com.plato.consumer.event.MessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 搜索扇出服务 (Job C)
 * 
 * 职责：将新消息同步到 Elasticsearch，支持全文搜索
 * 
 * 核心逻辑：
 * 1. 接收消息事件
 * 2. 转换为 Elasticsearch 文档
 * 3. 写入 Elasticsearch 索引
 * 4. 处理消息状态更新（撤回/删除）
 * 
 * @author hc
 * @since 2025/11/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchFanoutService {

    private final MessageSearchRepository messageSearchRepository;

    /**
     * 执行搜索扇出（将消息写入 Elasticsearch）
     * 
     * @param event 消息事件
     */
    public void fanout(MessageEvent event) {
        try {
            Long msgId = event.getMsgId();
            Long sessionId = event.getSessionId();
            Long seqId = event.getSeqId();

            log.debug("Starting search fanout: msgId={}, sessionId={}, seqId={}", 
                      msgId, sessionId, seqId);

            // ========== 构建 Elasticsearch 文档 ==========
            MessageDocument document = buildDocument(event);

            // ========== 写入 Elasticsearch ==========
            messageSearchRepository.save(document);

            log.info("Search fanout completed: msgId={}, sessionId={}", msgId, sessionId);

        } catch (Exception e) {
            log.error("Search fanout failed: event={}", event, e);
            // 注意：搜索扇出失败不应该影响主流程，所以只记录日志不抛异常
        }
    }

    /**
     * 处理消息状态更新（撤回/删除）
     * 
     * @param event 消息事件
     */
    public void handleStatusUpdate(MessageEvent event) {
        try {
            Long msgId = event.getMsgId();
            Integer newStatus = event.getStatus();

            log.debug("Handling message status update in ES: msgId={}, newStatus={}", 
                      msgId, newStatus);

            if (newStatus == 2) {
                // ========== 删除状态：从索引中删除 ==========
                messageSearchRepository.deleteById(msgId);
                log.info("Deleted message from ES index: msgId={}", msgId);
            } else {
                // ========== 撤回状态：更新文档状态 ==========
                messageSearchRepository.findById(msgId).ifPresent(doc -> {
                    doc.setStatus(newStatus);
                    doc.setIndexedTimeMs(System.currentTimeMillis());
                    messageSearchRepository.save(doc);
                    log.info("Updated message status in ES: msgId={}, newStatus={}", 
                             msgId, newStatus);
                });
            }

        } catch (Exception e) {
            log.error("Failed to handle message status update in ES: event={}", event, e);
        }
    }

    /**
     * 构建 Elasticsearch 文档
     * 
     * @param event 消息事件
     * @return Elasticsearch 文档
     */
    private MessageDocument buildDocument(MessageEvent event) {
        // 获取创建时间戳
        Long createTimeMs = event.getCreateTime() != null 
                ? event.getCreateTime().toEpochSecond(java.time.ZoneOffset.UTC) * 1000
                : System.currentTimeMillis();

        return MessageDocument.builder()
                .msgId(event.getMsgId())
                .sessionId(event.getSessionId())
                .seqId(event.getSeqId())
                .senderId(event.getSenderId())
                .msgType(event.getMsgType())
                .content(extractSearchableContent(event))
                .status(event.getStatus() != null ? event.getStatus() : 0)
                .createTimeMs(createTimeMs)
                .indexedTimeMs(System.currentTimeMillis())
                .build();
    }

    /**
     * 提取可搜索的内容
     * 
     * 根据消息类型提取合适的搜索内容：
     * - 文本消息：直接使用内容
     * - 图片/视频/文件：提取文件名或描述
     * - 其他类型：返回类型描述
     * 
     * @param event 消息事件
     * @return 可搜索的内容
     */
    private String extractSearchableContent(MessageEvent event) {
        String content = event.getContent();
        Integer msgType = event.getMsgType();

        if (content == null || content.isEmpty()) {
            return getMessageTypeDescription(msgType);
        }

        // 对于文本消息，直接返回内容
        if (msgType == 1) {
            return content;
        }

        // 对于富媒体消息，尝试从 JSON 中提取文件名或描述
        // 这里简化处理，实际项目中可能需要解析 JSON
        // 例如：{"filename": "报告.pdf", "url": "..."}
        try {
            // 如果内容中包含文件名等信息，可以提取出来
            // 这里简化处理，直接返回原内容
            return content + " " + getMessageTypeDescription(msgType);
        } catch (Exception e) {
            log.warn("Failed to extract searchable content: msgId={}", event.getMsgId(), e);
            return getMessageTypeDescription(msgType);
        }
    }

    /**
     * 获取消息类型描述（用于搜索）
     * 
     * @param msgType 消息类型
     * @return 类型描述
     */
    private String getMessageTypeDescription(Integer msgType) {
        return switch (msgType) {
            case 1 -> "文本";
            case 2 -> "图片";
            case 3 -> "视频";
            case 4 -> "文件";
            case 5 -> "语音";
            case 6 -> "位置";
            case 7 -> "链接";
            case 8 -> "表情";
            case 9 -> "系统消息";
            default -> "消息";
        };
    }
}

