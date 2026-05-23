package cn.refinex.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 订单管理服务
 *
 * @author refinex
 */
@Service
public class OrderManageService {

    Logger logger = LoggerFactory.getLogger(OrderManageService.class);

    /**
     * 根据订单号获取订单
     *
     * @param orderId 订单号
     * @return 订单
     */
    public String getOrderById(String orderId) {
        return "订单号：" + orderId;
    }

    /**
     * 退款
     *
     * @param orderId 订单号
     * @param reason  退款原因
     * @return 退款单号
     */
    public String refund(String orderId, String reason) {
        logger.info("退款订单号：{}，原因：{}", orderId, reason);
        logger.info("退款成功");
        return UUID.randomUUID().toString();
    }
}
