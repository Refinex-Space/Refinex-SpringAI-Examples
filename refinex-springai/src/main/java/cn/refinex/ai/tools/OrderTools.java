package cn.refinex.ai.tools;

import cn.refinex.ai.service.OrderManageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 *
 *
 * @author refinex
 */
@Component
public class OrderTools {

    Logger logger = LoggerFactory.getLogger(OrderTools.class);

    private final OrderManageService orderManageService;

    public OrderTools(OrderManageService orderManageService) {
        this.orderManageService = orderManageService;
    }

    @Tool(name = "apply_refund", description = "根据用户传入的订单信息发起退款")
    public String applyRefund(
            @ToolParam(description = "订单编号，为数字类型") String orderId,
            @ToolParam(description = "商品名称") String name,
            @ToolParam(description = "退款原因") String reason) {
        logger.info("已为商品: {}，订单号: {}申请退款 , 退款原因： {}", name, orderId, reason);

        orderManageService.refund(orderId, reason);

        return "已为商品: " + name + "，订单号: " + orderId + "申请退款 , 退款原因： " + reason;
    }
}
