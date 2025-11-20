package com.plato.storage.repository.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * MyBatis TypeHandler for JSON Map<String, String>
 * 
 * 将数据库中的 JSON 字符串与 Java Map 对象互转
 * 
 * @author hc
 * @since 2025/11/19
 */
@MappedTypes(Map.class)
public class JsonMapTypeHandler extends BaseTypeHandler<Map<String, String>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, String> parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setString(i, OBJECT_MAPPER.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert Map to JSON string", e);
        }
    }

    @Override
    public Map<String, String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }

    @Override
    public Map<String, String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJson(json);
    }

    @Override
    public Map<String, String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJson(json);
    }

    private Map<String, String> parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON string to Map", e);
        }
    }
}
