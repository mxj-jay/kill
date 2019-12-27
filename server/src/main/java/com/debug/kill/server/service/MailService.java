package com.debug.kill.server.service;

import com.debug.kill.server.dto.MailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

/**
 * @author Damocles
 */
@Service
@EnableAsync
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    @Autowired
    private Environment env;

    @Autowired
    private JavaMailSender javaMailSender;



    @Async
    public void sendSimpleMail(MailDto mailDto){
        try {
            SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
            simpleMailMessage.setFrom(env.getProperty("mail.send.from"));
            simpleMailMessage.setTo(mailDto.getTos());
            simpleMailMessage.setSubject(mailDto.getSubject());
            simpleMailMessage.setText(mailDto.getContent());
            javaMailSender.send(simpleMailMessage);

            logger.info("发送简单文本--发送成功!");
        }catch (Exception e){
            logger.error("发送简单文本文件--发送异常:",e.fillInStackTrace());
        }
    }

    @Async
    public void sendHtmlMail(MailDto mailDto){
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage,true,"utf-8");
            mimeMessageHelper.setFrom(env.getProperty("mail.send.from"));
            mimeMessageHelper.setTo(mailDto.getTos());
            mimeMessageHelper.setSubject(mailDto.getSubject());
            mimeMessageHelper.setText(mailDto.getContent(),true);
            javaMailSender.send(mimeMessage);

            logger.info("发送Html文本--发送成功!");
        }catch (Exception e){
            logger.error("发送Html文本文件--发送异常:",e.fillInStackTrace());
        }
    }

}
