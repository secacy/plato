package com.plato.consumer.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 消息预览工具类
 * 
 * @author hc
 * @since 2025/11/20
 */
@Slf4j
public class MessagePreviewUtil {

    private static final int MAX_PREVIEW_LENGTH = 50;

    /**
     * 生成消息预览文本
     * 
     * @param msgType 消息类型
     * @param content 消息内容
     * @return 预览文本
     */
    public static String generatePreview(Integer msgType, String content) {
        if (msgType == null) {
            return "[消息]";
        }

        switch (msgType) {
            case 1: // 文本消息
                return generateTextPreview(content);
            
            case 2: // 图片
                return "[图片]";
            
            case 3: // 视频
                return "[视频]";
            
            case 4: // 文件
                return "[文件]";
            
            case 5: // 语音
                return "[语音]";
            
            case 6: // 位置
                return "[位置]";
            
            case 7: // 名片
                return "[名片]";
            
            case 8: // 红包
                return "[红包]";
            
            default:
                return "[消息]";
        }
    }

    /**
     * 生成文本消息预览
     * 
     * @param content 消息内容
     * @return 预览文本
     */
    private static String generateTextPreview(String content) {
        if (content == null || content.isEmpty()) {
            return "[空消息]";
        }

        // 去除换行符
        String preview = content.replaceAll("[\n\r]", " ");

        // 截取前 N 个字符
        if (preview.length() > MAX_PREVIEW_LENGTH) {
            return preview.substring(0, MAX_PREVIEW_LENGTH) + "...";
        }

        return preview;
    }
}

