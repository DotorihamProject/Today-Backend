package com.today.todayproject.domain.user.service;

import com.today.todayproject.domain.user.User;
import com.today.todayproject.domain.user.dto.UserNicknameUpdateRequestDto;
import com.today.todayproject.domain.user.dto.UserPasswordUpdateRequestDto;
import com.today.todayproject.domain.user.repository.UserRepository;
import com.today.todayproject.global.BaseException;
import com.today.todayproject.global.BaseResponseStatus;
import com.today.todayproject.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 닉네임 수정 로직
     */
    @Override
    public void updateNickname(UserNicknameUpdateRequestDto userNicknameUpdateRequestDto) throws Exception {
        User loginUser = userRepository.findByEmail(SecurityUtil.getLoginUserEmail())
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_LOGIN_USER));

        String originalNickname = loginUser.getNickname();
        String changeNickname = userNicknameUpdateRequestDto.getChangeNickname();

        // 기존 닉네임과 변경할 닉네임이 같을 때 예외 처리
        if(originalNickname == changeNickname) {
            throw new BaseException(BaseResponseStatus.SAME_NICKNAME);
        }

        loginUser.updateNickname(userNicknameUpdateRequestDto.getChangeNickname());
    }

    /**
     * 비밀번호 수정 로직
     */
    @Override
    public void updatePassword(UserPasswordUpdateRequestDto userPasswordUpdateRequestDto) throws Exception {
        User loginUser = userRepository.findByEmail(SecurityUtil.getLoginUserEmail())
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_LOGIN_USER));

        boolean isSamePassword = loginUser
                .matchPassword(passwordEncoder, userPasswordUpdateRequestDto.getCurrentPassword());
        if(!isSamePassword) throw new BaseException(BaseResponseStatus.WRONG_CURRENT_PASSWORD);
        loginUser.updatePassword(passwordEncoder, userPasswordUpdateRequestDto.getChangePassword());
    }

}
