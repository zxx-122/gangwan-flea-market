package org.example.gwtzsc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MessageRequest {
    @NotNull(message = "接收用户不能为空")
    private Long toUserId;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    private Long itemId;
}
