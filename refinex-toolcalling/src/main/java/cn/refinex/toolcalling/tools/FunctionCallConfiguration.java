package cn.refinex.toolcalling.tools;

import cn.refinex.toolcalling.service.TimeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * 函数调用配置
 *
 * @author refinex
 */
@Configuration
public class FunctionCallConfiguration {

    @Bean
    @Description("根据用户输入的时区获取该时区的当前时间")
    public Function<TimeService.Request, TimeService.Response> getTimeFunction(TimeService timeService) {
        return timeService::getTimeByZoneId;
    }
}
