package com.wlh.seckill.services;

import com.alibaba.fastjson.JSON;
import com.wlh.seckill.db.dao.OrderDao;
import com.wlh.seckill.db.dao.SeckillActivityDao;
import com.wlh.seckill.db.dao.SeckillCommodityDao;
import com.wlh.seckill.db.po.Order;
import com.wlh.seckill.db.po.SeckillActivity;
import com.wlh.seckill.db.po.SeckillCommodity;
import com.wlh.seckill.mq.RocketMQService;
import com.wlh.seckill.util.RedisService;
import com.wlh.seckill.util.SnowFlake;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;


@Slf4j
@Service
public class SeckillActivityService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    private RocketMQService rocketMQService;

    @Autowired
    SeckillCommodityDao seckillCommodityDao;

    @Autowired
    OrderDao orderDao;

    /**
     * datacenterId;  数据中心
     * machineId;     机器标识
     */
    private SnowFlake snowFlake = new SnowFlake(1, 1);

    /**
     * 判断秒杀库存
     * estimate inventory
     * @param activityId
     * @return
     */
    public boolean seckillStockValidator(long activityId) {
        String key = "stock:" + activityId;
        return redisService.stockDeductValidator(key);
    }

    /**
     * 创建订单
     * Create Order
     * @param seckillActivityId
     * @param userId
     * @return
     * @throws Exception
     */
    public Order createOrder(long seckillActivityId, long userId) throws Exception {
        /*
         * 1.创建订单
         * 1. Create Order
         */
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        Order order = new Order();
        //采用雪花算法生成订单ID
        //Using Snowflake Algorithm to Generate Order ID
        order.setOrderNo(String.valueOf(snowFlake.nextId()));
        order.setSeckillActivityId(seckillActivity.getId());
        order.setUserId(userId);
        order.setOrderAmount(seckillActivity.getSeckillPrice().longValue());
        /*
         *2.发送创建订单消息
         *2.Send create order message
         */
        rocketMQService.sendMessage("seckill_order", JSON.toJSONString(order));

        /*
         * 3.发送订单付款状态校验消息
         * 3.Send order payment status verification message
         * messageDelayLevel=1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
         */
        rocketMQService.sendDelayMessage("pay_check", JSON.toJSONString(order), 5);


        return order;
    }

    /**
     * 订单支付完成处理
     * Order payment completed
     * @param orderNo
     */
    public void payOrderProcess(String orderNo) throws Exception {
        //log.info("Complete the payment order  Order No.：" + orderNo);
        log.info("完成支付订单  订单号：" + orderNo);
        Order order = orderDao.queryOrder(orderNo);
        /*
         * 1.判断订单是否存在
         * 1.Check if an order exists
         * 2.判断订单状态是否为未支付状态
         * 2.Determine whether the order status is unpaid
         */
        if (order == null) {
            //log.error("The order number corresponding to the order does not exist：" + orderNo);
            log.error("订单号对应订单不存在：" + orderNo);
            return;
        } else if(order.getOrderStatus() != 1 ) {

            //og.error("Invalid order status：" + orderNo);
            log.error("订单状态无效：" + orderNo);
            return;
        }
        /*
         * 2.订单支付完成
         * 2. Order payment completed
         */
        order.setPayTime(new Date());
        //订单状态 0:没有可用库存，无效订单 1:已创建等待付款 ,2:支付完成
        //Order status 0: No stock available, invalid order
        //1: Created and waiting for payment , 2: Payment completed
        order.setOrderStatus(2);
        orderDao.updateOrder(order);
        /*
         *3.发送订单付款成功消息
         *3.Send order payment success message
         */
        rocketMQService.sendMessage("pay_done", JSON.toJSONString(order));
    }

    /**
     * 将秒杀详情相关信息倒入redis
     * Pour the information about the seckill details into redis
     * @param seckillActivityId
     */
    public void pushSeckillInfoToRedis(long seckillActivityId) {
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        redisService.setValue("seckillActivity:" + seckillActivityId, JSON.toJSONString(seckillActivity));

        SeckillCommodity seckillCommodity = seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());
        redisService.setValue("seckillCommodity:" + seckillActivity.getCommodityId(), JSON.toJSONString(seckillCommodity));
    }

}

