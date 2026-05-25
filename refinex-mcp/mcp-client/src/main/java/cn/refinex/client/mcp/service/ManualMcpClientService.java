package cn.refinex.client.mcp.service;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 手动调用 MCP 服务
 *
 * @author refinex
 */
@Service
public class ManualMcpClientService {

    private final OpenAiChatModel chatModel;
    private ChatClient chatClient;

    public ManualMcpClientService(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostConstruct
    public void init() {
        // STDIO
        // 第一步：创建 Transport，传入连接参数
        ServerParameters parameters = ServerParameters.builder("java")
                .args("-jar", "/Users/refinex/develop/project/Refinex-SpringAI-Examples/refinex-mcp/mcp-server-stdio/target/mcp-server-stdio-1.0.0-SNAPSHOT.jar")
                .build();
        StdioClientTransport stdioTransport = new StdioClientTransport(parameters, McpJsonMapper.createDefault());
        // 第二步：构建 McpSyncClient
        McpSyncClient stdioClient = McpClient.sync(stdioTransport)
                // 设置客户端信息
                .clientInfo(new McpSchema.Implementation("refinex-client", "1.0"))
                // 设置请求超时时间
                .requestTimeout(Duration.ofSeconds(10))
                .build();
        // 第三步：完成握手
        stdioClient.initialize();

        // SSE
        // 第一步：创建 Transport，传入连接参数
        HttpClientSseClientTransport transport = HttpClientSseClientTransport
                .builder("http://127.0.0.1:8082")
                .sseEndpoint("/sse")
                .build();
        // 第二步：构建 McpSyncClient
        McpSyncClient sseClient = McpClient.sync(transport)
                // 设置客户端信息
                .clientInfo(new McpSchema.Implementation("sse-client", "1.0"))
                // 设置请求超时时间
                .requestTimeout(Duration.ofSeconds(10))
                .build();
        // 第三步：完成握手
        sseClient.initialize();

        // STREAMABLE
        // 第一步：创建 Transport，传入连接参数
        HttpClientStreamableHttpTransport streamableTransport = HttpClientStreamableHttpTransport
                .builder("http://127.0.0.1:8083")
                .endpoint("/mcp")
                .build();
        // 第二步：构建 McpSyncClient
        McpSyncClient streamableClient = McpClient.sync(streamableTransport)
                // 设置客户端信息
                .clientInfo(new McpSchema.Implementation("streamable-client", "1.0"))
                // 设置请求超时时间
                .requestTimeout(Duration.ofSeconds(10))
                .build();
        // 第三步：完成握手
        streamableClient.initialize();

        // 工具回调提供者：用于将 MCP 客户端注册为工具回调
        List<McpSyncClient> clients = List.of(streamableClient);
        SyncMcpToolCallbackProvider provider = SyncMcpToolCallbackProvider.builder()
                .mcpClients(clients)
                .build();

        // 获取工具回调
        ToolCallback[] callbacks = provider.getToolCallbacks();
        // 创建 ChatClient 并设置默认工具回调
        this.chatClient = ChatClient.builder(chatModel)
                .defaultToolCallbacks(callbacks)
                .build();
    }

    /**
     * 智能体调用
     */
    public String chat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }

    public static void main(String[] args) {
        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder("http://127.0.0.1:8082/")
                .sseEndpoint("/sse")
                .build();
        McpSyncClient sseClient = McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("sse-client", "1.0"))
                .requestTimeout(Duration.ofSeconds(10))
                .build();
        sseClient.initialize();
    }
}
