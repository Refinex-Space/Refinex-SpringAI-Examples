package cn.refinex.langchain4j.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * 对话记忆服务
 *
 * @author refinex
 */
@AiService
public interface LangChainMemoryAiService {

    /**
     * 对话记忆
     *
     * @param memoryId     记忆ID
     * @param userMessage  用户消息
     * @return             机器人回复
     */
    String chatMemory(@MemoryId String memoryId, @UserMessage String userMessage);
}
