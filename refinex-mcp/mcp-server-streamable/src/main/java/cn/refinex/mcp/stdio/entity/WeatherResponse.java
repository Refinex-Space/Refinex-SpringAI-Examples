package cn.refinex.mcp.stdio.entity;

import lombok.Data;

/**
 * 天气响应
 *
 * @author refinex
 */
@Data
public class WeatherResponse {

    /**
     * 城市
     */
    private String city;

    /**
     * 日期
     */
    private String date;

    /**
     * 区县
     */
    private String i;

    /**
     * 街道
     */
    private String s;

    /**
     * 描述
     */
    private String description;

    /**
     * 温度
     */
    private double temperature;

    /**
     * 构造函数
     *
     * @param city 城市
     * @param date 日期
     * @param i 区县
     * @param s 街道
     * @param description 描述
     * @param temperature 温度
     */
    public WeatherResponse(String city, String date, String i, String s, String description, double temperature) {
        this.city = city;
        this.date = date;
        this.i = i;
        this.s = s;
        this.description = description;
        this.temperature = temperature;
    }
}
