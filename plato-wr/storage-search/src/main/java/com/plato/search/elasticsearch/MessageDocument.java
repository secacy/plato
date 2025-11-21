package com.plato.search.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

/**
 * Elasticsearch 消息文档
 * 
 * 索引名称：im_messages
 * 用途：支持全文搜索和复杂过滤查询
 * 
 * @author hc
 * @since 2025/11/20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "im_messages")
@Setting(shards = 3, replicas = 1)
public class MessageDocument {

    /**
     * 文档ID（使用 msg_id）
     */
    @Id
    private Long msgId;

    /**
     * 会话ID（用于过滤）
     */
    @Field(type = FieldType.Long)
    private Long sessionId;

    /**
     * 会话内序列ID（用于排序和跳转）
     */
    @Field(type = FieldType.Long)
    private Long seqId;

    /**
     * 发送者用户ID（用于过滤）
     */
    @Field(type = FieldType.Long)
    private Long senderId;

    /**
     * 消息类型（用于过滤）
     * 1=文本, 2=图片, 3=视频, 4=文件, 5=语音, 6=位置, 7=链接, 8=表情, 9=系统消息
     */
    @Field(type = FieldType.Integer)
    private Integer msgType;

    /**
     * 消息内容（全文搜索字段）
     * 使用 IK 分词器进行中文分词
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    /**
     * 消息状态
     * 0=正常, 1=撤回, 2=删除
     */
    @Field(type = FieldType.Integer)
    private Integer status;

    /**
     * 创建时间（毫秒时间戳，用于时间范围过滤和排序）
     */
    @Field(type = FieldType.Long)
    private Long createTimeMs;

    /**
     * 索引更新时间（毫秒时间戳）
     */
    @Field(type = FieldType.Long)
    private Long indexedTimeMs;
}

