package com.plato.consumer.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * 事件解析器
 * 
 * 职责：解析 TiCDC Canal JSON 格式的事件，转换为内部 MessageEvent
 * 
 * @author hc
 * @since 2025/11/20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventParser {

    private final ObjectMapper objectMapper;

    /**
     * 解析事件
     * 
     * @param eventBytes 事件字节数组
     * @return 消息事件（如果解析失败或不是 message 表事件，返回 null）
     */
    public MessageEvent parse(byte[] eventBytes) {
        try {
            // 1. 尝试解析为 TiCDC Canal JSON 格式
            String json = new String(eventBytes);
            TiCDCEvent cdcEvent = objectMapper.readValue(json, TiCDCEvent.class);

            // 2. 检查是否是 message 表的事件
            if (!"message".equals(cdcEvent.getTable())) {
                log.debug("Ignoring non-message table event: table={}", cdcEvent.getTable());
                return null;
            }

            // 3. 检查是否是 DDL 事件
            if (Boolean.TRUE.equals(cdcEvent.getIsDdl())) {
                log.debug("Ignoring DDL event");
                return null;
            }

            // 4. 检查是否有数据
            if (cdcEvent.getData() == null || cdcEvent.getData().isEmpty()) {
                log.warn("No data in CDC event");
                return null;
            }

            // 5. 提取第一条数据（通常只有一条）
            Map<String, Object> data = cdcEvent.getData().get(0);

            // 6. 构建 MessageEvent
            return buildMessageEvent(cdcEvent.getType(), data, cdcEvent.getEventTimestamp());

        } catch (Exception e) {
            log.error("Failed to parse CDC event", e);
            return null;
        }
    }

    /**
     * 构建消息事件
     * 
     * @param eventType      事件类型
     * @param data           数据
     * @param eventTimestamp 事件时间戳（秒）
     * @return 消息事件
     */
    private MessageEvent buildMessageEvent(String eventType, Map<String, Object> data, Long eventTimestamp) {
        try {
            // 提取字段
            Long sessionId = getLong(data, "session_id");
            Long seqId = getLong(data, "seq_id");
            Long msgId = getLong(data, "msg_id");
            Long senderId = getLong(data, "sender_id");
            Integer msgType = getInteger(data, "msg_type");
            String content = getString(data, "content");
            Integer status = getInteger(data, "status");

            // 提取时间（TiCDC 返回的是字符串格式的时间）
            LocalDateTime createTime = parseTimestamp(data.get("create_time"));

            // 构建事件
            return MessageEvent.builder()
                    .eventType(eventType)
                    .sessionId(sessionId)
                    .seqId(seqId)
                    .msgId(msgId)
                    .senderId(senderId)
                    .msgType(msgType)
                    .content(content)
                    .status(status)
                    .createTime(createTime)
                    .eventTimestamp(eventTimestamp != null ? eventTimestamp * 1000 : System.currentTimeMillis())
                    .build();

        } catch (Exception e) {
            log.error("Failed to build MessageEvent from data: {}", data, e);
            return null;
        }
    }

    /**
     * 从 Map 中获取 Long 值
     */
    private Long getLong(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        return null;
    }

    /**
     * 从 Map 中获取 Integer 值
     */
    private Integer getInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        return null;
    }

    /**
     * 从 Map 中获取 String 值
     */
    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 解析时间戳
     * 
     * TiCDC 返回的时间格式可能是：
     * - "2025-11-20 10:30:00"
     * - "2025-11-20T10:30:00Z"
     * - Unix 时间戳（毫秒）
     */
    private LocalDateTime parseTimestamp(Object timestampObj) {
        if (timestampObj == null) {
            return null;
        }

        try {
            // 如果是数字，按 Unix 时间戳（毫秒）处理
            if (timestampObj instanceof Number) {
                long millis = ((Number) timestampObj).longValue();
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
            }

            // 如果是字符串，尝试解析
            String timestampStr = timestampObj.toString();

            // 尝试解析 ISO 格式
            if (timestampStr.contains("T")) {
                return LocalDateTime.parse(timestampStr.replace("Z", ""));
            }

            // 尝试解析标准格式 "yyyy-MM-dd HH:mm:ss"
            timestampStr = timestampStr.replace(" ", "T");
            return LocalDateTime.parse(timestampStr);

        } catch (Exception e) {
            log.error("Failed to parse timestamp: {}", timestampObj, e);
            return null;
        }
    }
}

