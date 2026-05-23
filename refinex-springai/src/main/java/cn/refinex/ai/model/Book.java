package cn.refinex.ai.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.math.BigDecimal;

/**
 * @param title       书名
 * @param author      作者
 * @param description 简介
 * @param price       价格
 * @author refinex
 */
public record Book(
        @JsonPropertyDescription("书名") String title,
        @JsonPropertyDescription("作者") String author,
        @JsonPropertyDescription("简介") String description,
        @JsonPropertyDescription("价格") BigDecimal price) {
}
