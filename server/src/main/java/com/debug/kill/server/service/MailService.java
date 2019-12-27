package com.debug.kill.server.service;

import com.debug.kill.server.dto.MailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

/**
 * @author Damocles
 */
@Service
@EnableAsync
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    @Async
    public void sendSimpleMail(MailDto mailDto){
        try {

            logger.info("发送简单文本--发送成功!");
        }catch (Exception e){
            logger.error("发送简单文本文件--发送异常:",e.fillInStackTrace());
        }
    }

}
