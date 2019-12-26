package com.debug.kill.server.dto;

import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 前端传递的数据
 * @author Damocles
 */
@Data
public class KillDto implements Serializable {

    @NotNull
    private Integer killId;

    private Integer userId;
}