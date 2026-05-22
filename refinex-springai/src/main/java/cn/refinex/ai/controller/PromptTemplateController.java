package cn.refinex.ai.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * 提示模板控制器
 *
 * @author refinex
 */
@RestController
@RequestMapping("/prompt/template")
public class PromptTemplateController implements InitializingBean {

    private final ChatModel dashScopeModel;
    private ChatClient chatClient;

    /**
     * 构造注入
     *
     * @param dashScopeModel 聊天模型
     */
    public PromptTemplateController(ChatModel dashScopeModel) {
        this.dashScopeModel = dashScopeModel;
    }

    /**
     * 提示模板1
     *
     * @param message  用户消息
     * @param response 响应
     * @return 聊天流
     */
    @GetMapping("/promptsEngineer1")
    public Flux<String> promptsEngineer1(@RequestParam(value = "message") String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/event-stream");

        // 创建提示模板
        PromptTemplate promptTemplate = new PromptTemplate("请给我推荐几个关于{topic}的开源项目");
        // 方式1: 使用 add 方法添加参数
        // promptTemplate.add("topic", message);

        // 方式2：使用 create 方法添加参数（推荐）
        return chatClient.prompt(promptTemplate.create(Map.of("topic", message)))
                .system("你是一个专业的GitHub项目调研人员")
                .stream().content();
    }

    /**
     * 提示模板2
     *
     * @param message  用户消息
     * @param response 响应
     * @return 聊天流
     */
    @GetMapping("/promptsEngineer2")
    public Flux<String> promptsEngineer2(@RequestParam(value = "message") String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/event-stream");

        HashMap<String, Object> variables = new HashMap<>();
        variables.put("language", "Java");
        variables.put("topic", message);

        PromptTemplate promptTemplate = PromptTemplate.builder()
                .template("请给我推荐几个关于{topic}的开源项目,要求是和编程语言{language}相关的。")
                .variables(variables)
                .build();

        return chatClient.prompt(promptTemplate.create())
                .system("你是一个专业的GitHub项目调研人员")
                .stream().content();
    }

    /**
     * 提示模板3 - 自定义占位符分隔符
     *
     * @param message  用户消息
     * @param response 响应
     * @return 聊天流
     */
    @GetMapping("/promptsEngineer3")
    public Flux<String> promptsEngineer3(@RequestParam(value = "message") String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/event-stream");

        HashMap<String, Object> variables = new HashMap<>();
        variables.put("language", "Java");
        variables.put("topic", message);

        PromptTemplate promptTemplate = PromptTemplate.builder()
                // 自定义占位符分隔符
                .renderer(StTemplateRenderer.builder()
                        // 自定义开始占位符分隔符
                        .startDelimiterToken('<')
                        // 自定义结束占位符分隔符
                        .endDelimiterToken('>')
                        .build())
                // 使用自定义占位符分隔符
                .template("请给我推荐几个关于<topic>的开源项目,要求是和编程语言<language>相关的。")
                .variables(variables)
                .build();

        return chatClient.prompt(promptTemplate.create())
                .system("你是一个专业的GitHub项目调研人员")
                .stream().content();
    }

    @Value("classpath:prompts/open-source-system-prompt.st")
    private Resource systemPrompt;

    /**
     * 提示模板4 - 配置文件管理提示词
     *
     * @param message  用户消息
     * @param response 响应
     * @return 聊天流
     */
    @GetMapping("/promptsEngineer4")
    public Flux<String> promptsEngineer4(@RequestParam(value = "message") String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/event-stream");

        PromptTemplate promptTemplate = PromptTemplate.builder()
                // 使用资源文件
                .resource(systemPrompt)
                .build();

        return chatClient.prompt(promptTemplate.create(Map.of("topic", message)))
                .system("你是一个专业的GitHub项目调研人员")
                .stream().content();
    }

    /**
     * 初始化 聊天客户端
     */
    @Override
    public void afterPropertiesSet() {
        chatClient = ChatClient.builder(dashScopeModel)
                .defaultOptions(DashScopeChatOptions.builder()
                        .temperature(0.7)
                        .build())
                .build();
    }
}
