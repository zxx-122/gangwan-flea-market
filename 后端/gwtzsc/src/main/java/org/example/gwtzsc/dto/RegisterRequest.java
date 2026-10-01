package org.example.gwtzsc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 20, message = "账号长度需3-20位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需6-20位")
    private String password;

    private String nickname;

    private String phone;

    /** 可选：填了手机号且传了 smsCode 时校验短信验证码 */
    private String smsCode;
}