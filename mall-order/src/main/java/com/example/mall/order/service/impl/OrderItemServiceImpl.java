package com.example.mall.order.service.impl;

import com.example.mall.order.entity.OrderReturnReasonEntity;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.mall.order.dao.OrderItemDao;
import com.example.mall.order.entity.OrderItemEntity;
import com.example.mall.order.service.OrderItemService;


@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * RabbitMQ接收消息测试
     * 一个消息只能被一个客户端接受（可以复制多个本服务，然后在测试中一次发送多个消息，可以看到该服务打印的消息少于等于总消息数，注意启动测试类也可能消费一定的消息）
     * 只有一个消息完全处理完，才能接受到下一个消息
     * 使用@RabbitListener，必须开启@EnableRabbit
     * 如果要接受不同种类的消息，可以为类标注@RabbitListener，在不同方法上标注@RabbitHandler，以处理不同种类的消息
     */
    @RabbitListener(queues = {"hello-java-queue"})
    public void reviveMessage(Message message,
                              OrderReturnReasonEntity content) {
        // 拿到主体内容
        byte[] body = message.getBody();
        // 拿到的消息头属性信息
        MessageProperties messageProperties = message.getMessageProperties();
        System.out.println("接受到的消息...内容" + message + "===内容：" + content);
    }

}