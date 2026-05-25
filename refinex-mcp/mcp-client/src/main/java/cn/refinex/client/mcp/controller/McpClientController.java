package cn.refinex.client.mcp.controller;

import cn.refinex.client.mcp.service.McpClientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP 客户端控制器
 *
 * @author refinex
 */
@RestController
@RequestMapping("/mcp")
public class McpClientController {

    private final McpClientService mcpClientService;

    public McpClientController(McpClientService mcpClientService) {
        this.mcpClientService = mcpClientService;
    }

    @GetMapping("/callTool")
    public Object callTool(@RequestParam("type") String type) {
        return mcpClientService.callTool(type);
    }

    @GetMapping("/chat")
    public String chat(@RequestParam("query") String query) {
        return mcpClientService.chat(query);
    }
}
