package cn.refinex.toolcalling.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 *
 * @author refinex
 */
@Service
public class TimeService {

    Logger logger = LoggerFactory.getLogger(TimeService.class);

    /**
     * 根据时区获取时间
     * @param request 获取时间请求
     * @return 获取时间响应
     */
    public Response getTimeByZoneId(Request request) {
        logger.info("getTimeByZoneId invoke..., zoneId = {}", request.zoneId);
        ZoneId zoneId = ZoneId.of(request.zoneId);
        ZonedDateTime zonedDateTime = ZonedDateTime.now(zoneId);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        return new Response(zonedDateTime.format(dateTimeFormatter));
    }

    /**
     * 获取时间请求
     */
    public record Request(
            @JsonProperty(required = true, value = "zoneId")
            @JsonPropertyDescription("时区，例如：Asia/Shanghai") String zoneId
    ) {}

    /**
     * 获取时间响应
     */
    public record Response(String time) {}
}
