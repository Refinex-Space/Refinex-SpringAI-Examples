package cn.refinex.httpclient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;

/**
 * Spring AI HttpClient 调用方式
 *
 * @author refinex
 */
public class HttpClientCaller {

    private static final Logger logger = Logger.getLogger(HttpClientCaller.class.getName());

    private static final String API_KEY = System.getenv("DASHSCOPE_API_KEY");
    private static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    public static void main(String[] args) throws IOException, InterruptedException {
        // 验证 API Key 是否存在
        if (API_KEY == null || API_KEY.isBlank()) {
            logger.severe("错误：未设置环境变量 DASHSCOPE_API_KEY");
            logger.info("请通过以下方式设置：");
            logger.info("  Linux/Mac: export DASHSCOPE_API_KEY=your_api_key");
            logger.info("  Windows:   set DASHSCOPE_API_KEY=your_api_key");
            System.exit(1);
        }

        // 构建请求体
        String requestBody = """
                {
                    "model": "qwen-plus",
                    "messages": [
                        {
                            "role": "system",
                            "content": "You are a helpful assistant."
                        },
                        {
                            "role": "user",
                            "content": "介绍一下你自己."
                        }
                    ],
                    "stream": true
                }
                """;

        // 创建 HttpClient 实例
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            // 创建 HttpRequest 实例
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("X-DashScope-SEE", "enable")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // 发送请求并获取响应
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            String body = httpResponse.body();
            logger.info(body);
        }
    }
}
