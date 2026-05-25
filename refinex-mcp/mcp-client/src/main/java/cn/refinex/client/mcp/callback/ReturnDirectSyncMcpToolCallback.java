package cn.refinex.client.mcp.callback;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * 直接返回 MCP 工具回调
 *
 * @author refinex
 */
public class ReturnDirectSyncMcpToolCallback extends SyncMcpToolCallback {

    /**
     * 是否跳过 MCP 模型总结，默认 false
     */
    private final boolean returnDirect;

    /**
     * @param client       MCP 客户端
     * @param tool         MCP 工具
     * @param returnDirect 是否跳过 MCP 模型总结，默认 false
     */
    public ReturnDirectSyncMcpToolCallback(McpSyncClient client, McpSchema.Tool tool, boolean returnDirect) {
        super(client, tool);
        this.returnDirect = returnDirect;
    }

    /**
     * @return 是否跳过 MCP 模型总结，默认 false
     */
    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder()
                .returnDirect(returnDirect)
                .build();
    }
}
