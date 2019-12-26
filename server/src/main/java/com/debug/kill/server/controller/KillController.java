package com.debug.kill.server.controller;

import com.debug.kill.api.enums.StatusCode;
import com.debug.kill.api.response.BaseResponse;
import com.debug.kill.server.dto.KillDto;
import com.debug.kill.server.service.KillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

/**
 * 秒杀逻辑实现
 * Controller --> Service --> Mapper
 * @author 59742
 */
@RestController
public class KillController {

    private static final Logger logger = LoggerFactory.getLogger(KillController.class);

    private static final String prefix = "kill";

    @Autowired
    private KillService killService;


    /**
     * 商品秒杀核心逻辑
     *
     * @param result
     * @param dto
     * @return
     */
    @RequestMapping(value = prefix + "/execute", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public BaseResponse execute(@RequestBody @Validated KillDto dto, BindingResult result) {
        if (result.hasErrors() || dto.getKillId() <= 0) {
            return new BaseResponse(StatusCode.InvalidParams);
        }
        //TODO: 简单验证用户是否登陆

        BaseResponse response = new BaseResponse(StatusCode.Success);
        try {
            Boolean res = killService.killItem(dto.getKillId(), dto.getUserId());
            if (!res) {
                return new BaseResponse(StatusCode.Fail.getCode(), "哈哈~商品已抢购完毕或者不在抢购时间段哦!");
            }
        } catch (Exception e) {
            response = new BaseResponse(StatusCode.Fail.getCode(), e.getMessage());
        }
        return response;
    }


}
