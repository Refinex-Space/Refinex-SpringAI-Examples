package cn.refinex.ai.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Ollama 聊天控制器
 *
 * @author refinex
 */
@RestController
@RequestMapping("/ollama")
public class OllamaChatController {

//    @Autowired
//    @Qualifier("ollamaChatModel")
//    private ChatModel ollamaChatModel;
//
//    @GetMapping("/stream/chat")
//    public Flux<String> streamChat(@RequestParam(value = "message") String message, HttpServletResponse response) {
//        response.setCharacterEncoding("UTF-8");
//        response.setContentType("text/event-stream");
//
//        return ollamaChatModel.stream(new Prompt(message))
//                .map(resp -> resp.getResult().getOutput().getText());
//    }
}
