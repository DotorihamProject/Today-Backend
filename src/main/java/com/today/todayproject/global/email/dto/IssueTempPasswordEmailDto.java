package com.today.todayproject.global.email.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueTempPasswordEmailDto {

    private String userEmail;
    private String title;
    private String content;
}
