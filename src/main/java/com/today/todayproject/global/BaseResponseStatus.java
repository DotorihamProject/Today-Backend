package com.today.todayproject.global;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 에러 코드 관리
 */
@Getter
@AllArgsConstructor
public enum BaseResponseStatus {

    /**
     * 1000 : 요청 성공
     */
    SUCCESS(true, 1000, "요청에 성공하였습니다."),

    /**
     * 2000 : Request 오류
     */
    EXIST_EMAIL(false, 2000, "이미 존재하는 이메일입니다."),
    EXIST_NICKNAME(false, 2001, "이미 존재하는 닉네임입니다."),
    NOT_FOUND_LOGIN_USER(false, 2002, "로그인한 유저가 존재하지 않습니다."),
    NOT_FOUND_EMAIL(false, 2003, "해당 이메일이 존재하지 않습니다."),
    SAME_NICKNAME(false, 2004, "기존 닉네임과 같은 닉네임입니다.");


    private final boolean isSuccess;
    private final int code;
    private final String message;

}