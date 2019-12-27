package com.debug.kill.server.service;

import com.debug.kill.model.dto.KillSuccessUserInfo;
import com.debug.kill.model.entity.ItemKillSuccess;
import com.debug.kill.model.mapper.ItemKillSuccessMapper;
import com.debug.kill.server.dto.MailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ接收消息服务
 *
 * @author Damocles
*/
@Service
public class RabbitReceiverService {

    public static final Logger logger= LoggerFactory.getLogger(RabbitReceiverService.class);

    @RabbitListener(queues = {"${mq.kill.item.success.email.queue}"}, containerFactory = "singleListenerContainer")
    public void consumeEmailMsg(KillSuccessUserInfo info) {
        try {
            logger.info("秒杀异步邮件通知-秒杀信息:{}", info);

            //TODO: 真正的发送邮件逻辑



        }catch (Exception e){
            logger.error("秒杀异步邮件通知-接收消息-发生异常：",e.fillInStackTrace());
        }
    }


}












