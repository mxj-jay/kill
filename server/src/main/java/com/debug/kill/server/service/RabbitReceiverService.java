package com.debug.kill.server.service;

import com.debug.kill.model.dto.KillSuccessUserInfo;
import com.debug.kill.model.entity.ItemKillSuccess;
import com.debug.kill.model.mapper.ItemKillSuccessMapper;
import com.debug.kill.server.dto.MailDto;
import org.apache.commons.lang3.StringUtils;
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

    @Autowired
    private Environment env;

    @Autowired
    private MailService mailService;

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    /**
     * 异步邮件通知--接收消息
     * @param info
     */
    @RabbitListener(queues = {"${mq.kill.item.success.email.queue}"}, containerFactory = "singleListenerContainer")
    public void consumeEmailMsg(KillSuccessUserInfo info) {
        try {
            logger.info("秒杀异步邮件通知-秒杀信息:{}", info);

            //TODO: 真正的发送邮件逻辑 -- 封装一个MailDto进行数据传输
//            MailDto mailDto = new MailDto(env.getProperty("mail.kill.item.success.subject"),"这是一个简单文本~",new String[]{info.getEmail()});
//            mailService.sendSimpleMail(mailDto);
            String content = String.format(env.getProperty("mail.kill.item.success.content"),info.getItemName(),info.getCode());
            MailDto mailDto = new MailDto(env.getProperty("mail.kill.item.success.subject"),content,new String[]{info.getEmail()});
            mailService.sendHtmlMail(mailDto);

        }catch (Exception e){
            logger.error("秒杀异步邮件通知-接收消息-发生异常：",e.fillInStackTrace());
        }
    }

    /**
     * 秒杀成功后超时未支付-监听者
     * 消息的接收方绑定真正的消费队列
     * @param info
     */
    @RabbitListener(queues = {"${mq.kill.item.success.kill.dead.real.queue}"}, containerFactory = "singleListenerContainer")
    public void consumeExpireOrder(KillSuccessUserInfo info) {
        try {
            logger.info("用户秒杀成功后超时未支付-监听者-接收消息:{}", info);

            if (info!=null){
                KillSuccessUserInfo entity = itemKillSuccessMapper.selectByCode(info.getCode());

                if (entity!=null && entity.getStatus().intValue()==0){
                    itemKillSuccessMapper.expireOrder(info.getCode());
                }
            }

        }catch (Exception e){
            logger.error("用户秒杀成功后超时未支付-监听者-发生异常：",e.fillInStackTrace());
        }

    }


}












