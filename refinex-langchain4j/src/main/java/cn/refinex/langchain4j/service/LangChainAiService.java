package cn.refinex.langchain4j.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import java.awt.print.Book;

/**
 * 语言模型服务
 *
 * @author refinex
 */
@AiService
public interface LangChainAiService {

    /**
     * 聊天
     *
     * @param userMessage 用户消息
     * @return 机器人回复
     */
    String chat(String userMessage);

    /**
     * 流式聊天
     *
     * @param userMessages 用户消息
     * @return 机器人回复
     */
    Flux<String> chatStream(String userMessages);

    /**
     * 流式聊天
     *
     * @param topic 用户消息
     * @return 机器人回复
     */
    @SystemMessage("You are a helpful assistant.")
    @UserMessage("针对用户的内容：{{topic}}，先复述一遍他的问题，然后再回答")
    Flux<String> chatStream1(String topic);

    /**
     * 流式聊天
     *
     * @param topic 用户消息
     * @return 机器人回复
     */
    @UserMessage(fromResource = "your-prompt-template.txt")
    Flux<String> chatStream2(String topic);

    /**
     * 推荐图书 - 结构化输出
     *
     * @return 图书
     */
    @SystemMessage("你是一个专业的图书推荐人员")
    @UserMessage("请帮我推荐1本java相关的书")
    Book recommendBook();

}
