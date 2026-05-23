package cn.refinex.mcp.stdio.tools;

import cn.refinex.mcp.stdio.entity.WeatherRequest;
import cn.refinex.mcp.stdio.entity.WeatherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * 天气服务
 *
 * @author refinex
 */
@Service
public class WeatherService {

    Logger logger = LoggerFactory.getLogger(WeatherService.class);

    @Tool(name = "getWeather", description = "根据城市名称查询天气信息")
    public String getWeather(@ToolParam(required = true, description = "城市名称") String city) {
        if (city == null) {
            return "请输入城市名称";
        }

        return switch (city) {
            case "北京" -> "北京: 晴, 25°C";
            case "上海" -> "上海: 多云, 22°C";
            case "深圳" -> "深圳: 小雨, 28°C";
            default -> city + ": 下雪, -20°C";
        };
    }

    @Tool(
            name = "query_weather_by_city&date",
            description = "根据城市和日期获取天气信息"
    )
    public WeatherResponse queryWeather(WeatherRequest request) {
        logger.info("queryWeather invoke... query weather for city: {}", request.getCity());

        try {
            // 模拟 API 调用
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.error("queryWeather error", e);
            throw new RuntimeException(e);
        }

        double temp = Math.random() * 15 + 10;
        return new WeatherResponse(
                request.getCity(),
                request.getDate(),
                request.getI(),
                request.getS(),
                "晴",
                temp
        );
    }
}
