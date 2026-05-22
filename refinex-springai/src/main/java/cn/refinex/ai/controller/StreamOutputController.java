package cn.refinex.ai.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 流式输出控制器
 *
 * @author refinex
 */
@RestController
@RequestMapping("/ai/output")
public class StreamOutputController implements InitializingBean {

    private final ChatModel dashScopeModel;
    private ChatClient chatClient;

    /**
     * 构造注入
     *
     * @param dashScopeModel 聊天模型
     */
    public StreamOutputController(ChatModel dashScopeModel) {
        this.dashScopeModel = dashScopeModel;
    }

    /**
     * 流式输出聊天
     *
     * @param message 消息
     * @param response 响应
     * @return Flux<String>
     */
    @RequestMapping("/stream/chat1")
    public Flux<String> streamChat1(@RequestParam(value = "message") String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/event-stream");

        // 构建用户消息和模型选项
        Prompt prompt = new Prompt(message, DashScopeChatOptions.builder()
                .model("qwen-plus")
                .build());

        // 聊天响应流
        Flux<ChatResponse> responseFlux = dashScopeModel.stream(prompt);
        // 将响应结果映射为文本
        return responseFlux.map(resp -> resp.getResult().getOutput().getText());
    }

    /**
     * 流式输出聊天
     *
     * @param message 消息
     * @param response 响应
     * @return Flux<String>
     */
    @RequestMapping("/stream/chat2")
    public Flux<String> streamChat2(@RequestParam(value = "message") String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/event-stream");

        // 构建用户消息和模型选项
        Prompt prompt = new Prompt(message, DashScopeChatOptions.builder()
                .model("qwen-plus")
                .build());

        // 聊天响应流
        Flux<ChatResponse> responseFlux = chatClient.prompt(prompt).stream().chatResponse();
        // 将响应结果映射为文本
        return responseFlux.map(resp -> resp.getResult().getOutput().getText());
    }

    /**
     * 初始化 聊天客户端
     */
    @Override
    public void afterPropertiesSet() {
        chatClient = ChatClient.builder(dashScopeModel).build();
    }
}
