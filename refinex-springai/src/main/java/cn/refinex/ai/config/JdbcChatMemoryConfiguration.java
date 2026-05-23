package cn.refinex.ai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JDBC聊天记忆配置
 *
 * @author refinex
 */
@Configuration
public class JdbcChatMemoryConfiguration {

    /**
     * 聊天记忆 Bean
     *
     * @param jdbcChatMemoryRepository jdbc聊天记忆仓库
     * @return 聊天记忆
     */
    @Bean
    ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .build();
    }
}
