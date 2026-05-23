package cn.refinex.langchain4j.controller;

import cn.refinex.langchain4j.tools.TemperatureTools;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.ToolChoice;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Langchain4j 低级别控制器
 *
 * @author refinex
 */
@RestController
@RequestMapping("/langchain/low")
public class LangChainLowLevelController {

    Logger logger = LoggerFactory.getLogger(LangChainLowLevelController.class);

    private final OpenAiStreamingChatModel openAiStreamingChatModel;
    private final OpenAiChatModel openAiChatModel;

    public LangChainLowLevelController(OpenAiStreamingChatModel openAiStreamingChatModel, OpenAiChatModel openAiChatModel) {
        this.openAiStreamingChatModel = openAiStreamingChatModel;
        this.openAiChatModel = openAiChatModel;
    }

    /**
     * 流式 Hello 接口
     * <p>
     * 访问路径: /streamHello
     * 返回类型: Flux<String> - Server-Sent Events (SSE) 流式响应
     * <p>
     * 前端调用示例（JavaScript）:
     * <pre>
     * const eventSource = new EventSource('/streamHello');
     * eventSource.onmessage = (event) => {
     *     console.log('收到:', event.data);  // 逐字输出
     * };
     * </pre>
     *
     * @param response HttpServletResponse，用于设置响应编码
     * @return Flux<String> 流式字符串响应
     */
    @RequestMapping("/streamHello")
    public Flux<String> streamHello(HttpServletResponse response) {
        // ============================================================
        // 步骤 1: 设置响应编码为 UTF-8
        // ============================================================
        // 确保中文字符正确传输，避免乱码问题
        response.setCharacterEncoding("UTF-8");

        // ============================================================
        // 步骤 2: 创建响应式 Flux（背压友好的异步数据流）
        // ============================================================
        // Flux.create(): 编程式创建 Flux，允许手动发射元素
        // fluxSink: 数据汇，用于向 Flux 推送数据
        return Flux.create(fluxSink -> {

            // ============================================================
            // 步骤 3: 调用流式 AI 模型
            // ============================================================
            // chat(): 非阻塞方法，立即返回，通过回调异步接收结果
            // 参数1: 用户消息内容
            // 参数2: 流式响应处理器，定义各阶段回调
            openAiStreamingChatModel.chat("你好，介绍一下你自己", new StreamingChatResponseHandler() {

                // --------------------------------------------------------
                // 回调 1: 部分响应（最重要） - 逐 token 接收
                // --------------------------------------------------------
                // 触发时机: 每次模型生成一个 token 时
                // 调用次数: N 次（N = 总 token 数）
                // 用途: 将每个 token 实时推送给前端
                @Override
                public void onPartialResponse(String partialResponse) {
                    // fluxSink.next(): 向 Flux 发射一个数据元素
                    // 前端会立即收到这个部分响应，实现逐字显示效果
                    fluxSink.next(partialResponse);
                }

                // --------------------------------------------------------
                // 回调 2: 完整响应 - 所有 token 都已发送完毕
                // --------------------------------------------------------
                // 触发时机: 模型完成全部响应生成后
                // 调用次数: 1 次
                // 用途: 通知前端流式传输结束
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    // fluxSink.complete(): 关闭数据流
                    // 前端会收到 stream 结束信号（EventSource 的 onclose 事件）
                    fluxSink.complete();
                }

                // --------------------------------------------------------
                // 回调 3: 错误处理 - 发生异常时调用
                // --------------------------------------------------------
                // 触发时机: 流式过程中发生错误（网络中断、API 失败等）
                // 用途: 将错误传播给 Flux，前端可捕获并显示错误信息
                @Override
                public void onError(Throwable error) {
                    // fluxSink.error(): 向 Flux 发射错误信号
                    // 前端会收到 error 事件，可以显示错误提示
                    fluxSink.error(error);
                }
            });
        });
    }

    /**
     * 手动维护对话记忆的示例接口
     * <p>
     * 演示如何通过手动维护 ChatMessage 列表来实现多轮对话记忆功能
     * 注意：这种方式需要自己管理消息列表，实际生产环境建议使用 ChatMemory 组件
     *
     * @param response HttpServletResponse，用于设置响应编码
     * @return 最后一轮对话中 AI 的回复内容
     */
    @RequestMapping("/memory")
    public String memory(HttpServletResponse response) {
        // 设置响应编码为 UTF-8，避免中文乱码
        response.setCharacterEncoding("UTF-8");

        // 创建消息列表，用于存储整个对话历史
        List<ChatMessage> messages = new ArrayList<>();

        // ============================================================
        // 第一轮对话
        // ============================================================
        // 添加系统消息：定义 AI 的角色和行为
        messages.add(new SystemMessage("你是一个 AI 助手"));
        // 添加用户消息：自我介绍
        messages.add(new UserMessage("我叫 Refinex，是一名 JAVA 程序员"));
        // 调用 AI 模型，获取回复
        AiMessage aiMessage = openAiChatModel.chat(messages).aiMessage();
        logger.info("aiMessage: {}", aiMessage);
        // 将 AI 的回复添加到消息列表，保持对话历史完整
        messages.add(aiMessage);

        // ============================================================
        // 第二轮对话
        // ============================================================
        // 添加用户消息：询问关于 Refinex 的信息
        messages.add(new UserMessage("Refinex 是干什么的？"));
        // 调用 AI 模型（此时 messages 包含前三轮的所有消息）
        AiMessage aiMessage2 = openAiChatModel.chat(messages).aiMessage();
        logger.info("aiMessage2: {}", aiMessage2);
        // 将 AI 的回复添加到消息列表，保持对话历史完整
        messages.add(aiMessage2);

        // ============================================================
        // 第三轮对话
        // ============================================================
        // 添加用户消息：测试 AI 是否记得用户身份
        messages.add(new UserMessage("我叫什么名字？"));
        // 调用 AI 模型（此时 messages 包含前面的所有消息）
        AiMessage aiMessage3 = openAiChatModel.chat(messages).aiMessage();
        logger.info("aiMessage3: {}", aiMessage3);
        // 将 AI 的回复添加到消息列表，保持对话历史完整
        messages.add(aiMessage3);

        // 返回最后一轮对话中 AI 的回复内容
        return aiMessage3.text();
    }

    /**
     * 使用 ChatMemory 组件管理对话记忆的示例接口
     *
     * 演示如何通过 LangChain4j 的 ChatMemory 组件实现多轮对话记忆功能，
     * 相比手动维护消息列表，ChatMemory 提供了：
     * - 自动的消息管理（如滑动窗口、过期清理）
     * - 可插拔的存储实现（内存/Redis/数据库）
     * - 内置的会话隔离能力
     *
     * @param response HttpServletResponse，用于设置响应编码
     * @return 最后一轮对话中 AI 的回复内容
     */
    @RequestMapping("/memory1")
    public String memory1(HttpServletResponse response) {
        // 设置响应编码为 UTF-8，避免中文乱码
        response.setCharacterEncoding("UTF-8");

        // ============================================================
        // 初始化 ChatMemory（对话记忆组件）
        // ============================================================
        // MessageWindowChatMemory: 基于消息窗口的记忆实现
        // withMaxMessages(10): 设置最多保留 10 条消息，超出后自动淘汰最早的消息
        // 这种滑动窗口策略可以控制上下文长度，避免 Token 超限
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        // ============================================================
        // 第一轮对话
        // ============================================================
        // 添加系统消息到对话记忆
        chatMemory.add(new SystemMessage("你是一个AI助手"));
        // 添加用户消息到对话记忆
        chatMemory.add(new UserMessage("我叫 Refinex，是一个 JAVA 程序员"));
        // 从 ChatMemory 获取完整的消息历史，调用 AI 模型
        AiMessage answer = openAiChatModel.chat(chatMemory.messages()).aiMessage();
        // 打印 AI 回复到控制台
        logger.info("answer: {}", answer);
        // 将 AI 的回复添加到对话记忆，保持历史完整
        chatMemory.add(answer);

        // ============================================================
        // 第二轮对话
        // ============================================================
        // 添加用户消息：询问关于 Refinex 的信息
        chatMemory.add(new UserMessage("Refinex 是干什么的?"));
        // 再次调用 AI 模型（此时 chatMemory.messages() 自动包含全部历史）
        AiMessage answer1 = openAiChatModel.chat(chatMemory.messages()).aiMessage();
        logger.info("answer1: {}", answer1);
        // 将 AI 的回复添加到对话记忆，保持历史完整
        chatMemory.add(answer1);
        chatMemory.add(answer1);

        // ============================================================
        // 第三轮对话
        // ============================================================
        // 添加用户消息：测试 AI 是否记得用户身份
        chatMemory.add(new UserMessage("我是谁？"));
        AiMessage answer2 = openAiChatModel.chat(chatMemory.messages()).aiMessage();
        logger.info("answer2: {}", answer2);
        // 将 AI 的回复添加到对话记忆，保持历史完整
        chatMemory.add(answer2);

        // 返回最后一轮的 AI 回复文本
        return answer2.text();
    }

    /**
     * 结构化输出（JSON Schema）示例接口
     *
     * 演示如何使用 ResponseFormat 强制 AI 模型以指定的 JSON 格式返回结果，
     * 这对于需要将 AI 输出与后端系统集成的场景非常有用。
     *
     * 适用场景：
     * - 数据提取（如从文本中提取实体信息）
     * - API 调用前的参数准备
     * - 与其他系统的数据对接
     *
     * @return 以 JSON 格式返回的结构化数据字符串
     */
    @RequestMapping("/structure")
    public String structure() {
        // ============================================================
        // 步骤 1: 构建 JSON Schema（定义输出结构）
        // ============================================================
        // ResponseFormat: 响应格式配置，支持 TEXT 或 JSON
        ResponseFormat responseFormat = ResponseFormat.builder()
                // 设置响应类型为 JSON（默认为 TEXT）
                .type(ResponseFormatType.JSON)
                // 配置 JSON Schema，严格约束输出格式
                .jsonSchema(JsonSchema.builder()
                        // OpenAI 要求为 schema 指定名称（必需字段）
                        .name("Person")
                        // 定义根元素的 JSON Schema
                        .rootElement(JsonObjectSchema.builder()
                                // 添加字符串类型属性: name（姓名）
                                .addStringProperty("name")
                                // 添加整数类型属性: age（年龄）
                                .addIntegerProperty("age")
                                // 添加数字类型属性: height（身高，支持小数）
                                .addNumberProperty("height")
                                // 添加布尔类型属性: married（婚姻状况）
                                .addBooleanProperty("married")
                                // 必填字段列表：指定哪些字段必须存在
                                .required("name", "age", "height", "married")
                                .build())
                        .build())
                .build();

        // ============================================================
        // 步骤 2: 构建请求（包含原始文本和格式要求）
        // ============================================================
        ChatRequest chatRequest = ChatRequest.builder()
                // 设置响应格式（告知 AI 必须以指定 JSON 输出）
                .responseFormat(responseFormat)
                .messages(UserMessage.from("""
                        John is 42 years old and lives an independent life.
                        He stands 1.75 meters tall and carries himself with confidence.
                        Currently unmarried, he enjoys the freedom to focus on his personal goals and interests.
                        output in json format
                        """))
                .build();

        // ============================================================
        // 步骤 3: 调用模型并返回结果
        // ============================================================
        // 模型会自动提取文本中的信息，并按照 JSON Schema 格式返回
        return openAiChatModel.chat(chatRequest).aiMessage().text();
    }

    @RequestMapping("tool")
    public String tool() {
        // 1. 定义工具列表
        // 从 TemperatureTools 类中提取所有 @Tool 注解的方法作为可用工具
        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(TemperatureTools.class);

        // 2. 构造用户提示词
        UserMessage userMessage = UserMessage.from("2026年05月23日，上海的气温怎样？");

        // 维护完整对话历史
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(userMessage);

        // 3. 创建 ChatRequest，并指定工具列表
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(chatMessages)
                // 指定工具列表
                .toolSpecifications(toolSpecifications)
                // 自动选择工具
                .toolChoice(ToolChoice.AUTO)
                .build();

        // 4. 调用模型（第一轮）
        ChatResponse response = openAiChatModel.chat(chatRequest);
        AiMessage aiMessage = response.aiMessage();

        // 5. 把模型返回的消息添加到对话历史中
        chatMessages.add(aiMessage);

        // 6. 执行工具调用
        // 获取模型请求执行的工具列表（可能为空）
        List<ToolExecutionRequest> toolExecutionRequests = response.aiMessage().toolExecutionRequests();
        toolExecutionRequests.forEach(toolExecutionRequest -> {
            // 创建工具执行器，绑定工具实例
            ToolExecutor toolExecutor = new DefaultToolExecutor(new TemperatureTools(), toolExecutionRequest);
            logger.info("toolExecutor: {}", toolExecutor);

            // 执行工具，传入执行请求和会话ID
            String result = toolExecutor.execute(toolExecutionRequest, UUID.randomUUID().toString());

            // 将工具执行结果封装为消息对象
            ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage.from(toolExecutionRequest, result);

            // 7. 把工具执行结果添加到对话历史中
            chatMessages.add(toolExecutionResultMessage);
        });

        // 8. 重新构造 ChatRequest，使用完整的对话历史（包含工具执行结果）
        //    并再次指定工具列表，以便模型可以进行多轮工具调用
        ChatRequest finalRequest = ChatRequest.builder()
                // 包含：用户消息 + AI响应 + 工具执行结果
                .messages(chatMessages)
                // 再次提供工具定义
                .toolSpecifications(toolSpecifications)
                .build();

        // 9. 再次调用模型（第二轮）
        //    AI 会根据工具执行结果，生成面向用户的最终自然语言回答
        ChatResponse finalChatResponse = openAiChatModel.chat(finalRequest);
        return finalChatResponse.aiMessage().text();
    }
}
