package com.today.todayproject.global.email.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationCodeEmailSendDto {

    private String userEmail;
    private String title;
    private String content;
}
