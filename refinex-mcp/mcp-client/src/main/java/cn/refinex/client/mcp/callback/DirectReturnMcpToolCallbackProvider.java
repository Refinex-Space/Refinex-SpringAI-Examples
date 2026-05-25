package cn.refinex.client.mcp.callback;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.support.ToolUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 直接返回结果的 MCP 工具回调提供者
 *
 * @author refinex
 */
@Slf4j
public class DirectReturnMcpToolCallbackProvider extends SyncMcpToolCallbackProvider {

    /**
     * MCP 客户端列表
     */
    private final List<McpSyncClient> mcpClients;

    /**
     * 是否直接返回结果
     */
    private boolean returnDirect;

    /**
     * 构造函数
     *
     * @param mcpClients   MCP 客户端列表
     * @param returnDirect 是否直接返回结果
     */
    public DirectReturnMcpToolCallbackProvider(List<McpSyncClient> mcpClients, boolean returnDirect) {
        super(mcpClients);
        this.mcpClients = mcpClients;
        this.returnDirect = returnDirect;
    }

    /**
     * 获取工具回调
     *
     * @return 工具回调
     */
    @Override
    public ToolCallback[] getToolCallbacks() {
        var toolCallbacks = new ArrayList<>();

        // 遍历处理每一个 MCP 客户端
        for (McpSyncClient mcpClient : mcpClients) {
            List<McpSchema.Tool> toolList = Collections.emptyList();

            try {
                // 获取 MCP 客户端中的工具列表
                toolList = mcpClient.listTools().tools();
            } catch (Exception e) {
                // 跳过该 MCP，继续处理其它的
                continue;
            }

            // 遍历工具列表，创建工具回调
            for (var tool : toolList) {
                toolCallbacks.add(new ReturnDirectSyncMcpToolCallback(mcpClient, tool, returnDirect));
            }
        }

        // 转换为数组并返回
        var array = toolCallbacks.toArray(new ToolCallback[0]);
        validateToolCallbacks(array);
        return array;
    }

    /**
     * 验证工具回调，确保没有重复的工具名称
     *
     * @param toolCallbacks 工具回调
     */
    private void validateToolCallbacks(ToolCallback[] toolCallbacks) {
        List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
        duplicateToolNames.forEach(s -> log.info("tool name found: {}", s));
        if (!duplicateToolNames.isEmpty()) {
            throw new IllegalStateException("Multiple tools with the same name (%s)".formatted(String.join(", ", duplicateToolNames)));
        }
    }
}
