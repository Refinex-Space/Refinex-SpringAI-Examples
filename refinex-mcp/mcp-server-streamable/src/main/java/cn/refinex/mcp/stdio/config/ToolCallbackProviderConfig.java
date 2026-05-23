package cn.refinex.mcp.stdio.config;

import cn.refinex.mcp.stdio.tools.WeatherService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ToolCallbackProvider 配置类
 *
 * @author refinex
 */
@Configuration
public class ToolCallbackProviderConfig {

    @Bean
    public ToolCallbackProvider weatherTools(WeatherService weatherService) {
        // 自动扫描 WeatherService 中带有 @Tool 注解的方法
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherService)
                .build();
    }
}
