package cn.refinex.ai.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 聊天客户端控制器
 *
 * @author refinex
 */
@RestController
@RequestMapping("/client")
public class ChatClientController implements InitializingBean {

    private final ChatModel dashScopeModel;
    private ChatClient chatClient;

    /**
     * 构造注入
     *
     * @param dashScopeModel    聊天模型
     */
    public ChatClientController(ChatModel dashScopeModel) {
        this.dashScopeModel = dashScopeModel;
    }

    /**
     * 简单调用
     *
     * @param message 要发送的消息
     * @return 回复消息
     */
    @GetMapping("/simpleCall")
    public String simpleCall(@RequestParam String message) {
        //return chatClient.prompt(message).call().content();
        return chatClient.prompt(message).system("请使用韩语回答").call().content();
    }

    /**
     * 流式调用
     *
     * @param message 要发送的消息
     * @return 回复消息
     */
    @GetMapping("/streamCall")
    public Flux<String> streamCall(@RequestParam String message) {
        return chatClient.prompt(message).stream().content();
    }

    /**
     * 初始化 聊天客户端
     *
     * @throws Exception 初始化异常
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        chatClient = ChatClient.builder(dashScopeModel)
                // 添加一个默认的顾问: 日志顾问
                .defaultAdvisors(new SimpleLoggerAdvisor())
                // 设置默认的系统提示
                .defaultSystem("请使用英语回答")
                // 设置默认的选项
                .defaultOptions(DashScopeChatOptions.builder()
                        // 设置温度参数
                        .temperature(0.7)
                        .build())
                .build();
    }
}
