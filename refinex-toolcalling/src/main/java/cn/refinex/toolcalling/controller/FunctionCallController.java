package cn.refinex.toolcalling.controller;

import cn.refinex.toolcalling.tools.TimeTools;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 函数调用控制器
 *
 * @author refinex
 */
@RestController
@RequestMapping("/function-call")
public class FunctionCallController {

    private final OpenAiChatModel chatModel;
    private ChatClient chatClient;

    public FunctionCallController(OpenAiChatModel chatModel, ChatClient.Builder chatClientBuilder) {
        this.chatModel = chatModel;
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/functionCall1")
    public Flux<String> functionCall1(HttpServletResponse response, String city) {
        return chatClient.prompt()
                .advisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
                // 方式1: 使用工具名称
                .toolNames("getTimeFunction")
                // 方式2: 使用工具对象
                //.tools(new TimeTools())
                .user(city + "现在几点了？")
                .stream().content();
    }
}
