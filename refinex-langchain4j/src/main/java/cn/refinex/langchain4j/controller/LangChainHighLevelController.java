package cn.refinex.langchain4j.controller;

import cn.refinex.langchain4j.chatmemory.RedisChatMemoryStore;
import cn.refinex.langchain4j.service.LangChainAiService;
import cn.refinex.langchain4j.service.LangChainMemoryAiService;
import cn.refinex.langchain4j.tools.TemperatureTools;
import com.alibaba.fastjson2.JSON;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.awt.print.Book;

/**
 * 语言模型高阶控制器
 *
 * @author refinex
 */
@RestController
@RequestMapping("/langchain/high")
public class LangChainHighLevelController implements InitializingBean {

    private final OpenAiChatModel openAiChatModel;
    private final LangChainAiService langChainAiService;
    private final RedisChatMemoryStore redisChatMemoryStore;
    private LangChainMemoryAiService langChainMemoryAiService;

    public LangChainHighLevelController(OpenAiChatModel openAiChatModel, LangChainAiService langChainAiService, RedisChatMemoryStore redisChatMemoryStore) {
        this.openAiChatModel = openAiChatModel;
        this.langChainAiService = langChainAiService;
        this.redisChatMemoryStore = redisChatMemoryStore;
    }

    @RequestMapping("/chat")
    public String chat(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return langChainAiService.chat("日本都有哪些美食？");
    }

    @RequestMapping("/chatStream")
    public Flux<String> chatStream(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return langChainAiService.chatStream("日本都有哪些美食？");
    }

    @RequestMapping("/structure")
    public String structure(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        Book books = langChainAiService.recommendBook();
        return JSON.toJSONString(books);
    }

    @RequestMapping("/memoryChat")
    public String memoryChat(HttpServletResponse response, String msg, String memoryId) {
        response.setCharacterEncoding("UTF-8");
        return langChainMemoryAiService.chatMemory(memoryId, msg);
    }

    @RequestMapping("/toolCalling")
    public String toolCalling(HttpServletResponse response, String msg) {
        response.setCharacterEncoding("UTF-8");

        LangChainAiService langChainAiService1 = AiServices.builder(LangChainAiService.class)
                .tools(new TemperatureTools())
                .chatModel(openAiChatModel)
                .build();

        return langChainAiService1.chat(msg);
    }

    /**
     * Bean 属性设置完成后的回调方法
     * 在 Spring 完成依赖注入后自动调用，用于初始化 AiServices 实例
     * <p>
     * 实现 InitializingBean 接口是为了确保 chatModel 和 streamingChatModel
     * 已经完成依赖注入后再构建 AiServices
     *
     * @throws Exception 初始化过程中可能抛出的异常
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 使用 AiServices 构建器创建 LangChainMemoryAiService 的代理实现
        langChainMemoryAiService = AiServices.builder(LangChainMemoryAiService.class)
                // 注入同步聊天模型
                .chatModel(openAiChatModel)
                // 注入流式聊天模型（可选，支持流式输出）
                //.streamingChatModel(streamingChatModel)
                // 配置对话记忆提供者：为每个 memoryId 创建独立的滑动窗口记忆
                // 每个会话最多保留 10 条消息，超出后自动淘汰最早的消息
                //.chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder().id(memoryId).maxMessages(10).chatMemoryStore(redisChatMemoryStore).build())
                .build();
    }
}
