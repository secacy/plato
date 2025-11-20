package com.plato.storage.repository.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis TypeHandler for JSON String
 * 
 * 用于处理数据库 JSON 类型字段与 Java String 对象的转换
 * 确保 String 内容被正确地序列化为 JSON 格式存储到数据库
 * 
 * @author hc
 * @since 2025/11/20
 */
public class JsonStringTypeHandler extends BaseTypeHandler<String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            // 将 String 作为 JSON 值进行序列化（会自动加上引号和转义）
            String json = OBJECT_MAPPER.writeValueAsString(parameter);
            ps.setString(i, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert String to JSON", e);
        }
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJson(json);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJson(json);
    }

    private String parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            // 从 JSON 格式反序列化回 String（会去掉引号和转义）
            return OBJECT_MAPPER.readValue(json, String.class);
        } catch (JsonProcessingException e) {
            // 如果解析失败，直接返回原始字符串
            return json;
        }
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        if (parameter == null) {
            ps.setNull(i, java.sql.Types.VARCHAR);
        } else {
            setNonNullParameter(ps, i, parameter, jdbcType);
        }
    }
}
