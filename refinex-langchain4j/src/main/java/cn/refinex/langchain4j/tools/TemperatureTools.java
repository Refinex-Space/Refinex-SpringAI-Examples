package cn.refinex.langchain4j.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 温度工具类
 *
 * @author refinex
 */
public class TemperatureTools {

    Logger logger = LoggerFactory.getLogger(TemperatureTools.class);

    @Tool(
            value = "Get temperature by city and date",
            name = "getTemperatureByCityAndDate"
    )
    public String getTemperatureByCityAndDate(
            @P("city for get temperature") String city,
            @P("date for get temperature") String date
    ) {
        logger.info("getTemperatureByCityAndDate invoke...");
        return "23℃";
    }
}
