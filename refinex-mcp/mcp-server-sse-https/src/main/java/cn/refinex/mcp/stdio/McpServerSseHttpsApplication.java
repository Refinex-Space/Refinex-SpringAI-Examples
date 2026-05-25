package cn.refinex.mcp.stdio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP Server SSE Application
 *
 * @author refinex
 */
@SpringBootApplication
public class McpServerSseHttpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerSseHttpsApplication.class, args);
    }
}
