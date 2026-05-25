package cn.refinex.client.mcp.service;

import cn.refinex.client.mcp.callback.DirectReturnMcpToolCallbackProvider;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 客户端服务
 *
 * @author refinex
 */
@Service
public class McpClientService {

    Logger logger = LoggerFactory.getLogger(McpClientService.class);

    @Autowired
    private List<McpSyncClient> mcpSyncClients;

    /**
     * 将 MCP Client 的工具注入到 ChatClient 中让 ChatClient 调用
     */
    @Autowired
    private SyncMcpToolCallbackProvider toolCallbackProvider;

    @Autowired
    private OpenAiChatModel openAiChatModel;

    private ChatClient chatClient;

    /**
     * 直接调用工具
     *
     * @param type 工具类型
     * @return 调用工具结果
     */
    public McpSchema.CallToolResult callTool(String type) {
        // 工具名称
        String toolName = "getWeather";

        // 调用工具的参数
        Map<String, Object> param = new HashMap<>();
        param.put("city", "北京");

        for (McpSyncClient client : mcpSyncClients) {
            // 获取当前客户端信息
            McpSchema.Implementation clientInfo = client.getClientInfo();
            logger.info("client info: {}", clientInfo);
            // 获取对应服务端信息
            McpSchema.Implementation serverInfo = client.getServerInfo();
            logger.info("server info: {}", serverInfo);

            try {
                // 判断当前客户端是否匹配
                if (clientInfo.title().contains(type)) {
                    logger.info("client title: {}", clientInfo.title());
                    logger.info("start call mcp tool");

                    // 构造工具调用请求
                    McpSchema.CallToolRequest toolRequest = McpSchema.CallToolRequest.builder()
                            // 指定工具名称
                            .name(toolName)
                            // 指定工具参数
                            .arguments(param)
                            .build();
                    // 调用工具
                    McpSchema.CallToolResult callToolResult = client.callTool(toolRequest);
                    logger.info("call mcp tool result: {}", callToolResult);

                    return callToolResult;
                }
            } catch (Exception e) {
                logger.error("call mcp tool error", e);
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    /**
     * 智能体调用
     *
     * @param userMessage 用户消息
     * @return 聊天结果
     */
    public String chat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        // 获取工具回调
        // ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();

        DirectReturnMcpToolCallbackProvider callbackProvider = new DirectReturnMcpToolCallbackProvider(mcpSyncClients, true);

        this.chatClient = ChatClient.builder(openAiChatModel)
                // 设置默认工具回调: 将 MCP Server 的工具回调注入到 ChatClient 中
                //.defaultToolCallbacks(toolCallbacks)
                .defaultToolCallbacks(callbackProvider)
                .build();
    }
}
