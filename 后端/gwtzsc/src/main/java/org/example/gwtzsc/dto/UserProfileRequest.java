package org.example.gwtzsc.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserProfileRequest {
    private String nickname;

    @Pattern(regexp = "^(1[3-9]\\d{9})?$", message = "手机号格式不正确")
    private String phone;
}
