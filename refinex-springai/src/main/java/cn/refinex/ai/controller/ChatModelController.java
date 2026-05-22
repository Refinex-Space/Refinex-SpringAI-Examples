package cn.refinex.ai.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 聊天模型控制器
 *
 * @author refinex
 */
@RestController
@RequestMapping("/model")
public class ChatModelController {

    /**
     * 聊天模型
     */
    private final DashScopeChatModel dashScopeChatModel;

    /**
     * 构造注入
     *
     * @param dashScopeChatModel 聊天模型
     */
    public ChatModelController(DashScopeChatModel dashScopeChatModel) {
        this.dashScopeChatModel = dashScopeChatModel;
    }

    /**
     * 调用聊天模型（同步）
     *
     * @param message 消息
     * @return 回复
     */
    @RequestMapping("/call/string")
    public String callString(@RequestParam String message) {
        return dashScopeChatModel.call(message);
    }

    /**
     * 调用聊天模型（流式）
     *
     * @param message 消息
     * @return 回复
     */
    @RequestMapping("/stream/string")
    public Flux<String> callStreamString(@RequestParam String message, HttpServletResponse response) {
        // 这里需要设置一下响应的字符编码为 UTF-8 避免中文乱码
        response.setCharacterEncoding("UTF-8");
        // 设置响应内容类型为 text/event-stream 以便客户端能够正确处理流式响应
        response.setContentType("text/event-stream");
        return dashScopeChatModel.stream(message);
    }
}
