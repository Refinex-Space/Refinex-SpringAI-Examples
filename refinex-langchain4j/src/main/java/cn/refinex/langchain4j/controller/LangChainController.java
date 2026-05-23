package cn.refinex.langchain4j.controller;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Langchain4j 控制器
 *
 * @author refinex
 */
@RestController
@RequestMapping("/langchain4j")
public class LangChainController {

    private final OpenAiChatModel openAiChatModel;

    /**
     * 构造函数
     *
     * @param openAiChatModel openAiChatModel
     */
    public LangChainController(OpenAiChatModel openAiChatModel) {
        this.openAiChatModel = openAiChatModel;
    }

    @RequestMapping("/hello")
    public String hello() {
        return openAiChatModel.chat("你好，介绍一下你自己");
    }
}
