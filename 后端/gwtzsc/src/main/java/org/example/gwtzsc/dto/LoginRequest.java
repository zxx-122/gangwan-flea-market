package org.example.gwtzsc.dto;

import lombok.Data;

@Data
public class LoginRequest {

    /** 账号密码登录 */
    private String username;
    private String password;

    /** 手机号验证码登录 */
    private String phone;
    private String smsCode;
}