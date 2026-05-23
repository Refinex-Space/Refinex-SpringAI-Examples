package cn.refinex.ai.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * JDBC聊天记忆控制器
 *
 * @author refinex
 */
@RestController
@RequestMapping("/jdbc/memory")
public class JdbcChatMemoryController implements InitializingBean {

    private final ChatModel dashScopeModel;
    private final ChatMemory jdbcChatMemory;
    private ChatClient chatClient;

    /**
     * 构造注入
     *
     * @param dashScopeModel 聊天模型
     */
    public JdbcChatMemoryController(ChatModel dashScopeModel, ChatMemory jdbcChatMemory) {
        this.dashScopeModel = dashScopeModel;
        this.jdbcChatMemory = jdbcChatMemory;
    }

    @GetMapping("/callDb")
    public Flux<String> callDb(
            @RequestParam(value = "message") String message,
            @RequestParam(value = "conversationId") String conversationId,
            HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/event-stream");

        return chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    /**
     * 初始化
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        this.chatClient = ChatClient.builder(dashScopeModel)
                .defaultAdvisors(
                        // 使用基于JDBC的聊天记忆
                        MessageChatMemoryAdvisor.builder(jdbcChatMemory).build(),
                        new SimpleLoggerAdvisor())
                .defaultOptions(DashScopeChatOptions.builder()
                        .topP(0.7).build())
                .build();
    }
}
