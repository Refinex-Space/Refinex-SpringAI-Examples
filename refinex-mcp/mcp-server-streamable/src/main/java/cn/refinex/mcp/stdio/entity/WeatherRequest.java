package cn.refinex.mcp.stdio.entity;

import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 天气请求
 *
 * @author refinex
 */
@Data
public class WeatherRequest {

    @ToolParam(description = "城市")
    private String city;

    @ToolParam(description = "日期")
    private String date;

    @ToolParam(description = "区县")
    private String i;

    @ToolParam(description = "街道")
    private String s;
}
