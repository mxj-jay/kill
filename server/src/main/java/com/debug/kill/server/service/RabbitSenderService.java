package com.debug.kill.server.service;

import com.debug.kill.model.dto.KillSuccessUserInfo;
import com.debug.kill.model.mapper.ItemKillSuccessMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.awt.event.WindowFocusListener;

/**
 * RabbitMQ发送邮件服务
 *
 * @author Damocles
 */
@Service
public class RabbitSenderService {

    public static final Logger logger = LoggerFactory.getLogger(RabbitSenderService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    @Autowired
    private Environment env;

    /**
     * 秒杀成功异步发送邮件通知
     *
     * @param orderNo
     */
    public void sendKillSuccessEmailMsg(String orderNo) {
        logger.info("秒杀成功异步发送邮件通知-准备发送消息{}", orderNo);

        try {
            if (StringUtils.isNotBlank(orderNo)) {
                KillSuccessUserInfo info = itemKillSuccessMapper.selectByCode(orderNo);
                if (info != null) {
                    //TODO:rabbitmq发送消息的逻辑
                    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
                    rabbitTemplate.setExchange(env.getProperty("mq.kill.item.success.email.exchange"));
                    rabbitTemplate.setRoutingKey(env.getProperty("mq.kill.item.success.email.routing.key"));

                    //TODO:将info充当消息发送至队列
                    rabbitTemplate.convertAndSend(info, new MessagePostProcessor() {
                        @Override
                        public Message postProcessMessage(Message message) throws AmqpException {
                            MessageProperties messageProperties = message.getMessageProperties();
                            messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            messageProperties.setHeader(AbstractJavaTypeMapper.DEFAULT_CONTENT_CLASSID_FIELD_NAME, KillSuccessUserInfo.class);
                            return message;
                        }
                    });
                }
            }
        } catch (Exception e) {
            logger.error("秒杀成功异步发送邮件通知消息-发生异常，消息为：{}", orderNo, e.fillInStackTrace());
        }
    }

    /**
     * 秒杀成功后生成抢购订单-发送信息入死信队列，等待着一定时间失效超时未支付的订单
     *
     * @param orderCode
     */
    public void sendKillSuccessOrderExpireMsg(String orderCode) {
        logger.info("秒杀成功后生成抢购订单-发送信息入死信队列-{}", orderCode);

        try {
            if (StringUtils.isNotBlank(orderCode)) {
                KillSuccessUserInfo info = itemKillSuccessMapper.selectByCode(orderCode);
                if (info != null) {
                    //TODO: 消息的发送者绑定普通的消息队列和routingkey
                    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
                    rabbitTemplate.setExchange(env.getProperty("mq.kill.item.success.kill.dead.prod.exchange"));
                    rabbitTemplate.setRoutingKey(env.getProperty("mq.kill.item.success.kill.dead.prod.routing.key"));
                    rabbitTemplate.convertAndSend(info, new MessagePostProcessor() {
                        @Override
                        public Message postProcessMessage(Message message) throws AmqpException {
                            MessageProperties messageProperties = message.getMessageProperties();
                            messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            messageProperties.setHeader(AbstractJavaTypeMapper.DEFAULT_CONTENT_CLASSID_FIELD_NAME, KillSuccessUserInfo.class);
                            // 设置TTL过期时间
                            messageProperties.setExpiration(env.getProperty("mq.kill.item.success.kill.expire"));
                            return message;
                        }
                    });
                }
            }
        } catch (Exception e) {
            logger.error("秒杀成功后生成抢购订单-发送信息入死信队列，等待着一定时间失效超时未支付的订单-发生异常，消息为：{}", orderCode, e.fillInStackTrace());
        }
    }

}




























