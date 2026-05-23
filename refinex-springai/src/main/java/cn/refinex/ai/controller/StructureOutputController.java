package cn.refinex.ai.controller;

import cn.refinex.ai.model.Book;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 结构化输出控制器
 *
 * @author refinex
 */
@RestController
@RequestMapping("/structure")
public class StructureOutputController implements InitializingBean {

    Logger logger = LoggerFactory.getLogger(getClass());

    private final ChatModel dashScopeModel;
    private ChatClient chatClient;

    /**
     * 构造注入
     *
     * @param dashScopeModel 聊天模型
     */
    public StructureOutputController(ChatModel dashScopeModel) {
        this.dashScopeModel = dashScopeModel;
    }

    @GetMapping("/chat")
    public Flux<String> chat(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/event-stream");

        // 定义结构化输出转换器
        BeanOutputConverter<Book> beanOutputConverter = new BeanOutputConverter<>(Book.class);

        // 定义提示模板
        PromptTemplate promptTemplate = new PromptTemplate("""
                请帮我推荐几本 JAVA 相关的书
                {format}
                """);

        // 传入结构化输出转换器的格式
        return chatClient.prompt(promptTemplate.create(Map.of("format", beanOutputConverter.getFormat())))
                .system("你是一个专业的图书推荐人员")
                .stream().content();
    }

    @GetMapping("/chat1")
    public String chat1(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");

        // 定义结构化输出转换器
        BeanOutputConverter<Book> beanOutputConverter = new BeanOutputConverter<>(Book.class);

        // 定义提示模板
        PromptTemplate promptTemplate = new PromptTemplate("""
                请帮我推荐几本 JAVA 相关的书
                {format}
                """);

        // 传入结构化输出转换器的格式拿到结果
        String result = chatClient.prompt(promptTemplate.create(Map.of("format", beanOutputConverter.getFormat())))
                .system("你是一个专业的图书推荐人员")
                .call()
                .content();

        // 将结果转换为 Book 对象
        Book book = null;
        if (result != null) {
            book = beanOutputConverter.convert(result);
            logger.info("book: {}", book);
        }

        return result;
    }

    @GetMapping("/chat2")
    public String chat2() {
        Book book = chatClient.prompt("请帮我推荐几本 JAVA 相关的书")
                .system("你是一个专业的书籍推荐人员")
                .call()
                .entity(Book.class);

        if (book != null) {
            return book.toString();
        }
        return "null";
    }

    /**
     * 初始化 聊天客户端
     */
    @Override
    public void afterPropertiesSet() {
        chatClient = ChatClient.builder(dashScopeModel).build();
    }
}
