package com.plato.consumer.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * TiCDC Canal JSON 格式事件
 * 
 * TiCDC 推送到 Kafka 的消息格式（Canal JSON 协议）
 * 
 * @author hc
 * @since 2025/11/20
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TiCDCEvent {

    /**
     * 数据库名
     */
    @JsonProperty("database")
    private String database;

    /**
     * 表名
     */
    @JsonProperty("table")
    private String table;

    /**
     * 主键字段名列表
     */
    @JsonProperty("pkNames")
    private List<String> pkNames;

    /**
     * 是否为 DDL 事件
     */
    @JsonProperty("isDdl")
    private Boolean isDdl;

    /**
     * 事件类型（INSERT, UPDATE, DELETE）
     */
    @JsonProperty("type")
    private String type;

    /**
     * 事件时间戳（秒）
     */
    @JsonProperty("es")
    private Long eventTimestamp;

    /**
     * 事务时间戳（秒）
     */
    @JsonProperty("ts")
    private Long transactionTimestamp;

    /**
     * SQL 语句（DDL 事件）
     */
    @JsonProperty("sql")
    private String sql;

    /**
     * 变更数据（INSERT/UPDATE）
     */
    @JsonProperty("data")
    private List<Map<String, Object>> data;

    /**
     * 变更前数据（UPDATE）
     */
    @JsonProperty("old")
    private List<Map<String, Object>> old;
}

