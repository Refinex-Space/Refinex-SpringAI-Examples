package cn.refinex.ai.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天记忆控制器
 *
 * @author refinex
 */
@RestController
@RequestMapping("/memory")
public class ChatMemoryController implements InitializingBean {

    Logger log = LoggerFactory.getLogger(getClass());

    private final ChatModel dashScopeModel;
    private ChatClient chatClient;

    /**
     * 构造注入
     *
     * @param dashScopeModel 聊天模型
     */
    public ChatMemoryController(ChatModel dashScopeModel) {
        this.dashScopeModel = dashScopeModel;
    }

    @GetMapping("/call1")
    public String call1(String message) {
        List<Message> messages = new ArrayList<>();

        // 模拟短期记忆对话历史
        messages.add(new SystemMessage("你是一个旅行推荐师"));
        messages.add(new UserMessage("我想去新疆玩"));
        messages.add(new AssistantMessage("好的，我知道了，你要去新疆，请问你准备什么时候去"));
        messages.add(new UserMessage("我准备元旦的时候去玩"));
        messages.add(new AssistantMessage("好的，请问你想玩那些内容？"));
        messages.add(new UserMessage("我喜欢自然风光"));

        // 使用对话历史进行对话
        Prompt prompt = new Prompt(messages);
        return chatClient.prompt(prompt)
                // 用户消息
                .user(message)
                .call()
                .content();
    }

    @GetMapping("/call2")
    public String call2() {
        List<Message> messages = new ArrayList<>();

        // 第一轮对话
        messages.add(new SystemMessage("你是一个游戏设计师"));
        messages.add(new UserMessage("我想设计一个回合制游戏"));
        ChatResponse chatResponse = dashScopeModel.call(new Prompt(messages));
        String content = chatResponse.getResult().getOutput().getText();
        log.info("chatResponse: {}", chatResponse);

        // 把 ASSISTANT 的回复添加到对话历史中
        if (content != null && !content.isEmpty()) {
            messages.add(new AssistantMessage(content));
        }

        // 第二轮对话
        messages.add(new UserMessage("我想让游戏有三种职业"));
        chatResponse = dashScopeModel.call(new Prompt(messages));
        content = chatResponse.getResult().getOutput().getText();
        log.info("chatResponse: {}", chatResponse);
        return content;
    }

    @GetMapping("/call3")
    public Flux<String> call3(
            @RequestParam(value = "message") String message,
            @RequestParam(value = "conversationId") String conversationId,
            HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/event-stream");

        return chatClient.prompt()
                .user(message)
                // 通过 Advisor 传递 conversationId
                // 属于同一个 conversationId 的对话会话会记忆
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    /**
     * 初始化聊天客户端
     *
     * @throws Exception 初始化异常
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 创建一个内存聊天记忆, 基于滑动窗口保留最多 20 条消息
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();

        chatClient = ChatClient.builder(dashScopeModel)
                .defaultAdvisors(
                        // 使用内存进行聊天记忆的 Advisor
                        MessageChatMemoryAdvisor.builder(memory).build(),
                        // 实现 Logger 的 Advisor
                        new SimpleLoggerAdvisor())
                .defaultOptions(DashScopeChatOptions.builder()
                        // 设置采样率为 0.7
                        .topP(0.7)
                        .build())
                .build();
    }
}
