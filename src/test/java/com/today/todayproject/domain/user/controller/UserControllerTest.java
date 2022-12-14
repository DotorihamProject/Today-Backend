package com.today.todayproject.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.today.todayproject.domain.user.Role;
import com.today.todayproject.domain.user.User;
import com.today.todayproject.domain.user.dto.UserSignUpRequestDto;
import com.today.todayproject.domain.user.dto.UserUpdateMyInfoRequestDto;
import com.today.todayproject.domain.user.repository.UserRepository;
import com.today.todayproject.domain.user.service.UserService;
import com.today.todayproject.global.BaseException;
import com.today.todayproject.global.BaseResponseStatus;
import com.today.todayproject.global.util.GenerateDummy;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Slf4j
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    EntityManager em;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserService userService;
    @Autowired
    UserRepository userRepository;

    private static final String BEARER = "Bearer ";

    private String email = "test1@gmail.com";
    private String password = "password1!";
    private String nickname = "KSH";

    @Value("${jwt.access.header}")
    private String accessHeader;

    @Value("${image.defaultProfileImageUrl}")
    private String defaultProfileImageUrl;

    void userAndFriendSetUp() throws Exception {
        saveUsersAndFriends();
    }

    private void saveUsersAndFriends() throws Exception {
        User user1 = GenerateDummy.generateDummyUser("test1@naver.com", "password1234!", "KSH1",
                "s3://imgUrl1", Role.USER);
        User user2 = GenerateDummy.generateDummyUser("test2@naver.com", "password1234!", "KSH2",
                "s3://imgUrl2", Role.USER);
        User user3 = GenerateDummy.generateDummyUser("test3@naver.com", "password1234!", "KSH3",
                "s3://imgUrl3", Role.USER);
        User user4 = GenerateDummy.generateDummyUser("test4@naver.com", "password1234!", "KSH4",
                "s3://imgUrl4", Role.USER);
        User user5 = GenerateDummy.generateDummyUser("test5@naver.com", "password1234!", "KSH5",
                "s3://imgUrl5", Role.USER);
        User user6 = GenerateDummy.generateDummyUser("test6@naver.com", "password1234!", "KSH6",
                "s3://imgUrl6", Role.USER);
        User user7 = GenerateDummy.generateDummyUser("test7@naver.com", "password1234!", "KSH7",
                "s3://imgUrl7", Role.USER);
        User user8 = GenerateDummy.generateDummyUser("test8@naver.com", "password1234!", "KSH8",
                "s3://imgUrl8", Role.USER);
        User user9 = GenerateDummy.generateDummyUser("test9@naver.com", "password1234!", "KSH9",
                "s3://imgUrl9", Role.USER);
        User user10 = GenerateDummy.generateDummyUser("test10@naver.com", "password1234!", "KSH10",
                "s3://imgUrl10", Role.USER);

        signUpDummyData(user1);
        signUpDummyData(user2);
        signUpDummyData(user3);
        signUpDummyData(user4);
        signUpDummyData(user5);
        signUpDummyData(user6);
        signUpDummyData(user7);
        signUpDummyData(user8);
        signUpDummyData(user9);
        signUpDummyData(user10);
        User findUser2 = userRepository.findByEmail("test2@naver.com").orElse(null);
        saveFriends(user1, findUser2);
    }

    private void saveFriends(User requestUser, User requestedUser) throws Exception {
        Long requestedUserId = requestedUser.getId();
        log.info("requestedUserId : {}", requestedUserId);
        String accessToken = getAccessTokenByLogin(requestUser.getEmail(), requestUser.getPassword());
        mockMvc.perform(
                post("/friend/add/{friendId}", requestedUserId)
                        .header(accessHeader, BEARER + accessToken))
                .andExpect(status().isOk());
    }

    private void signUpDummyData(User user) throws Exception {
        String signUpDto = objectMapper.writeValueAsString(
                new UserSignUpRequestDto(user.getEmail(), user.getPassword(), user.getNickname()));
        signUpNoProfileSuccess(generateSignUpDtoFile(signUpDto));
    }

    private MockMultipartFile generateMultipartFileImage() throws IOException {
        return new MockMultipartFile(
                "profileImg",
                "testImage1.jpeg",
                "jpeg",
                new FileInputStream("src/test/resources/testimage/testImage1.jpeg"));
    }

    private MockMultipartFile generateUpdateMultipartFileImage() throws IOException {
        return new MockMultipartFile(
                "profileImg",
                "testImage2.png",
                "png",
                new FileInputStream("src/test/resources/testimage/testImage2.png"));
    }


    private void signUpProfileSuccess(MockMultipartFile userSignUpRequestDto) throws Exception {
        mockMvc.perform(
                        multipart("/sign-up")
                                .file(generateMultipartFileImage())
                                .file(userSignUpRequestDto))
                .andExpect(status().isOk());
    }

    private void signUpNoProfileSuccess(MockMultipartFile userSignUpRequestDto) throws Exception {
        mockMvc.perform(
                        multipart("/sign-up")
                                .file(userSignUpRequestDto))
                .andExpect(status().isOk());
    }

    private void signUpFail(MockMultipartFile signUpDto, String errorMessage) throws Exception {
        mockMvc.perform(
                        multipart("/sign-up")
                                .file(generateMultipartFileImage())
                                .file(signUpDto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(errorMessage));
    }

    private MockMultipartFile generateSignUpDtoFile(String dto) {
        return new MockMultipartFile("userSignUpRequestDto",
                "userSignUpRequestDto", "application/json",
                dto.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile generateUpdateMyInfoDtoFile(String dto) {
        return new MockMultipartFile("userUpdateMyInfoRequestDto",
                "userUpdateMyInfoRequestDto", "application/json",
                dto.getBytes(StandardCharsets.UTF_8));
    }

    private String getAccessTokenByLogin(String email, String password) throws Exception {
        User user = userRepository.findByEmail(email).orElse(null);
        Map<String, String> userMap = new HashMap<>();
        userMap.put("email", email);
        userMap.put("password", password);

        MvcResult result = mockMvc.perform(
                        post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(userMap)))
                .andExpect(status().isOk()).andReturn();

        return result.getResponse().getHeader(accessHeader);
    }

    @Test
    void ?????????_??????_??????_???_??????_??????_??????() throws Exception {
        //given
        String signUpDto = objectMapper.writeValueAsString(new UserSignUpRequestDto(email, password, nickname));
        MockMultipartFile userSignUpRequestDto = new MockMultipartFile("userSignUpRequestDto",
                "userSignUpRequestDto", "application/json",
                signUpDto.getBytes(StandardCharsets.UTF_8));

        //when
        signUpProfileSuccess(userSignUpRequestDto);

        //then
        User findUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));
        assertThat(findUser).isNotNull();
        assertThat(findUser.getProfileImgUrl()).isNotNull();
        assertThat(findUser.getEmail()).isEqualTo(email);
        assertThat(passwordEncoder.matches(password, findUser.getPassword())).isTrue();
        assertThat(findUser.getNickname()).isEqualTo(nickname);
    }

    @Test
    void ?????????_??????_??????_???_??????_??????_??????() throws Exception {
        //given
        String signUpDto = objectMapper.writeValueAsString(new UserSignUpRequestDto(email, password, nickname));
        MockMultipartFile userSignUpRequestDto = new MockMultipartFile("userSignUpRequestDto",
                "userSignUpRequestDto", "application/json",
                signUpDto.getBytes(StandardCharsets.UTF_8));

        //when
        signUpNoProfileSuccess(userSignUpRequestDto);

        //then
        User findUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));
        assertThat(findUser).isNotNull();
        assertThat(findUser.getProfileImgUrl()).isEqualTo(defaultProfileImageUrl);
        assertThat(findUser.getEmail()).isEqualTo(email);
        assertThat(passwordEncoder.matches(password, findUser.getPassword())).isTrue();
        assertThat(findUser.getNickname()).isEqualTo(nickname);
    }

    @Test
    void ??????_??????_??????_????????????_null??????_??????_??????_??????() throws Exception {
        //given
        String emailNullDto = objectMapper.writeValueAsString(
                new UserSignUpRequestDto(null, password, nickname));
        String passwordNullDto = objectMapper.writeValueAsString(
                new UserSignUpRequestDto(email, null, nickname));
        String nicknameNullDto = objectMapper.writeValueAsString(
                new UserSignUpRequestDto(email, password, null));

        //when
        signUpFail(generateSignUpDtoFile(emailNullDto), "???????????? ??????????????????.");
        signUpFail(generateSignUpDtoFile(passwordNullDto), "??????????????? ??????????????????.");
        signUpFail(generateSignUpDtoFile(nicknameNullDto), "???????????? ??????????????????.");

        //then
        assertThat(userRepository.findAll().size()).isEqualTo(0);
    }

    @Test
    void ??????_??????_??????_????????????_???_?????????_??????_??????_??????() throws Exception {
        //given
        String emailNullDto = objectMapper.writeValueAsString(
                new UserSignUpRequestDto("", password, nickname));
        String passwordNullDto = objectMapper.writeValueAsString(
                new UserSignUpRequestDto(email, "", nickname));
        String nicknameNullDto = objectMapper.writeValueAsString(
                new UserSignUpRequestDto(email, password, ""));

        //when
        signUpFail(generateSignUpDtoFile(emailNullDto), "???????????? ??????????????????.");
        signUpFail(generateSignUpDtoFile(passwordNullDto), "??????????????? ??????????????????.");
        signUpFail(generateSignUpDtoFile(nicknameNullDto), "???????????? ??????????????????.");

        //then
        assertThat(userRepository.findAll().size()).isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"test.com", "12@aaaa", "test"})
    void ??????_??????_???_????????????_?????????_?????????_?????????_??????_??????_??????(String email) throws Exception {
        //given
        String wrongEmailRegexDto = objectMapper.writeValueAsString(
                new UserSignUpRequestDto(email, password, nickname));

        //when
        signUpFail(generateSignUpDtoFile(wrongEmailRegexDto), "????????? ????????? ?????? ??????????????????.");

        //then
        assertThat(userRepository.findAll().size()).isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a1!", "aaaaaaaaaa2", "22222222222!", "abbbbbbbbbbb"})
    void ??????_??????_???_???????????????_??????_??????_???????????????_?????????_8???_?????????_?????????_??????_??????_??????(String password) throws Exception {
        //given
        String wrongSizePasswordDto = objectMapper.writeValueAsString(
                new UserSignUpRequestDto(email, password, nickname));

        //when
        signUpFail(generateSignUpDtoFile(wrongSizePasswordDto),
                "??????????????? ??????, ??????, ??????????????? 1??? ?????? ????????? 8??? ????????????????????????.");

        //then
        assertThat(userRepository.findAll().size()).isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"11.", "11!", "!!!", "abc12@"})
    void ??????_??????_???_????????????_??????_??????_?????????_?????????_??????_??????_??????(String nickname) throws Exception {
        //given
        String wrongRegexNicknameDto = objectMapper.writeValueAsString(
                new UserSignUpRequestDto(email, password, nickname));

        //when
        signUpFail(generateSignUpDtoFile(wrongRegexNicknameDto), "???????????? ??????, ??????, ????????? ???????????????.");

        //then
        assertThat(userRepository.findAll().size()).isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "aabbccddeef"})
    void ??????_??????_???_????????????_2???_??????_8???_?????????_?????????_??????_??????_??????(String nickname) throws Exception {
        //given
        String wrongRegexNicknameDto = objectMapper.writeValueAsString(
                new UserSignUpRequestDto(email, password, nickname));

        //when
        signUpFail(generateSignUpDtoFile(wrongRegexNicknameDto), "???????????? 2??? ?????? 8??? ?????????????????????.");

        //then
        assertThat(userRepository.findAll().size()).isEqualTo(0);
    }

    @Test
    void ??????_??????_?????????_?????????_??????_??????_??????_??????() throws Exception {
        //given
        String signUpDto = objectMapper.writeValueAsString(new UserSignUpRequestDto(email, password, nickname));
        signUpProfileSuccess(generateSignUpDtoFile(signUpDto));
        String accessToken = getAccessTokenByLogin(email, password);
        User beforeUpdateUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));
        String beforeUpdateProfileImgUrl = beforeUpdateUser.getProfileImgUrl();

        //when
        mockMvc.perform(
                multipart(HttpMethod.PATCH, "/user/update-my-info")
                        .file(generateUpdateMultipartFileImage())
                        .param("changeNickname", nickname + "123")
                        .header(accessHeader, BEARER + accessToken))
                .andExpect(status().isOk());

        //then
        User afterUpdateUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));
        assertThat(afterUpdateUser.getNickname()).isNotEqualTo(nickname);
        assertThat(afterUpdateUser.getNickname()).isEqualTo(nickname+"123");
        assertThat(afterUpdateUser.getProfileImgUrl()).isNotEqualTo(beforeUpdateProfileImgUrl);
    }

    @Test
    void ??????_??????_????????????_??????_???_??????() throws Exception {
        //given
        String signUpDto = objectMapper.writeValueAsString(new UserSignUpRequestDto(email, password, nickname));
        signUpProfileSuccess(generateSignUpDtoFile(signUpDto));
        String accessToken = getAccessTokenByLogin(email, password);

        //when
        mockMvc.perform(
                        multipart(HttpMethod.PATCH, "/user/update-my-info")
                                .param("changeNickname", nickname + "123")
                                .header(accessHeader, BEARER + accessToken))
                .andExpect(status().isOk());

        //then
        User afterUpdateUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));
        assertThat(afterUpdateUser.getNickname()).isNotEqualTo(nickname);
        assertThat(afterUpdateUser.getNickname()).isEqualTo(nickname+"123");
    }

    @Test
    void ??????_??????_???????????????_??????_???_??????() throws Exception {
        //given
        String signUpDto = objectMapper.writeValueAsString(new UserSignUpRequestDto(email, password, nickname));
        signUpProfileSuccess(generateSignUpDtoFile(signUpDto));
        String accessToken = getAccessTokenByLogin(email, password);

        //when
        mockMvc.perform(
                        patch("/user/update-password")
                                .param("currentPassword", password)
                                .param("changePassword", password + "123")
                                .header(accessHeader, BEARER + accessToken))
                .andExpect(status().isOk());

        //then
        User afterUpdateUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));
        assertThat(passwordEncoder.matches(password + "123", afterUpdateUser.getPassword())).isTrue();
    }

    @Test
    void ??????_??????_?????????_?????????_??????_???_??????() throws Exception {
        //given
        String signUpDto = objectMapper.writeValueAsString(new UserSignUpRequestDto(email, password, nickname));
        signUpProfileSuccess(generateSignUpDtoFile(signUpDto));
        String accessToken = getAccessTokenByLogin(email, password);
        User beforeUpdateUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));
        String beforeUpdateProfileImgUrl = beforeUpdateUser.getProfileImgUrl();

        //when
        mockMvc.perform(
                        multipart(HttpMethod.PATCH, "/user/update-my-info")
                                .file(generateUpdateMultipartFileImage())
                                .header(accessHeader, BEARER + accessToken))
                .andExpect(status().isOk());

        //then
        User afterUpdateUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));
        assertThat(afterUpdateUser.getProfileImgUrl()).isNotEqualTo(beforeUpdateProfileImgUrl);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123!", "@@@", "aa-"})
    void ??????_??????_?????????_????????????_??????_??????_?????????_?????????_??????_??????(String changeNickname) throws Exception {
        //given
        String signUpDto = objectMapper.writeValueAsString(new UserSignUpRequestDto(email, password, nickname));
        signUpProfileSuccess(generateSignUpDtoFile(signUpDto));
        String accessToken = getAccessTokenByLogin(email, password);

        //when
        mockMvc.perform(
                        multipart(HttpMethod.PATCH, "/user/update-my-info")
                                .param("changeNickname", changeNickname)
                                .header(accessHeader, BEARER + accessToken))
                .andExpect(status().isBadRequest());

        //then
        User updateFailUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));
        assertThat(updateFailUser.getNickname()).isNotEqualTo(changeNickname);
        assertThat(updateFailUser.getNickname()).isEqualTo(nickname);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "???", "a", "??????????????????????????????????????????"})
    void ??????_??????_?????????_????????????_2???_??????_8???_?????????_?????????_??????_??????(String changeNickname) throws Exception {
        //given
        String signUpDto = objectMapper.writeValueAsString(new UserSignUpRequestDto(email, password, nickname));
        signUpProfileSuccess(generateSignUpDtoFile(signUpDto));
        String accessToken = getAccessTokenByLogin(email, password);

        //when
        mockMvc.perform(
                    multipart(HttpMethod.PATCH, "/user/update-my-info")
                            .param("changeNickname", changeNickname)
                            .header(accessHeader, BEARER + accessToken))
            .andExpect(status().isBadRequest());



        //then
        User updateFailUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));
        assertThat(updateFailUser.getNickname()).isNotEqualTo(changeNickname);
        assertThat(updateFailUser.getNickname()).isEqualTo(nickname);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1a!", "12a!", "123aa!"})
    void ??????_??????_?????????_???????????????_8???_?????????_?????????_??????_??????(String changePassword) throws Exception {
        //given
        String signUpDto = objectMapper.writeValueAsString(new UserSignUpRequestDto(email, password, nickname));
        signUpProfileSuccess(generateSignUpDtoFile(signUpDto));
        String accessToken = getAccessTokenByLogin(email, password);

        //when
        mockMvc.perform(
                        patch("/user/update-password")
                                .param("currentPassword", password)
                                .param("changePassword", changePassword)
                                .header(accessHeader, BEARER + accessToken))
                .andExpect(status().isBadRequest());

        //then
        User updateFailUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));
        assertThat(passwordEncoder.matches(changePassword, updateFailUser.getPassword())).isFalse();
        assertThat(passwordEncoder.matches(password, updateFailUser.getPassword())).isTrue();
    }

    @Test
    void ??????_??????_???_??????_??????_??????() throws Exception {
        //given
        userAndFriendSetUp();
        String accessToken = getAccessTokenByLogin("test1@naver.com", "password1234!");
        String searchUserNickname = "KSH";

        //when, then
        User loginUser = userRepository.findByEmail("test1@naver.com")
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));
        mockMvc.perform
                (get("/user/search")
                        .param("loginUserId", String.valueOf(loginUser.getId()))
                        .param("searchUserNickname", searchUserNickname)
                        .header(accessHeader, BEARER + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void ??????_??????_??????_???_??????_??????_??????() throws Exception {
        //given
        userAndFriendSetUp();
        String accessToken = getAccessTokenByLogin("test1@naver.com", "password1234!");
        String searchUserNickname = "KSH";

        User loginUser = userRepository.findByEmail("test1@naver.com")
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));
        User lastFriendUser = userRepository.findByEmail("test2@naver.com")
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));
        User lastUser = userRepository.findByEmail("test7@naver.com")
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));

        //when, then

        mockMvc.perform
                        (get("/user/search")
                                .param("loginUserId", String.valueOf(loginUser.getId()))
                                .param("lastFriendUserId", String.valueOf(lastFriendUser.getId()))
                                .param("lastUserId", String.valueOf(lastUser.getId()))
                                .param("searchUserNickname", searchUserNickname)
                                .header(accessHeader, BEARER + accessToken))
                .andExpect(status().isOk());
    }
}