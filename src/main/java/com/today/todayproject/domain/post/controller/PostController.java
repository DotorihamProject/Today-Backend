package com.today.todayproject.domain.post.controller;

import com.today.todayproject.domain.post.dto.*;
import com.today.todayproject.domain.post.service.PostService;
import com.today.todayproject.global.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {

    private final PostService postService;

    @PostMapping("/save")
    public BaseResponse<PostSaveResponseDto> save(
            @RequestPart PostSaveDto postSaveDto,
            @RequestPart(required = false) List<MultipartFile> uploadImgs,
            @RequestPart(required = false) List<MultipartFile> uploadVideos) throws Exception {
        PostSaveResponseDto postSaveResponseDto = postService.save(postSaveDto, uploadImgs, uploadVideos);
        return new BaseResponse<>(postSaveResponseDto);
    }

    @GetMapping("/{postId}/{userId}")
    public BaseResponse<PostInfoDto> getPostInfo(@PathVariable("postId") Long postId,
                                                 @PathVariable("userId") Long userId) throws Exception {
        PostInfoDto postInfoDto = postService.getPostInfo(postId, userId);
        return new BaseResponse<>(postInfoDto);
    }

    @GetMapping("/{userId}/{month}")
    public BaseResponse<PostGetMonthInfoDto> getPostByUserAndMonth(@PathVariable("userId") Long userId,
                                                                   @PathVariable("month") int month) {
        PostGetMonthInfoDto userMonthPostInfo = postService.getUserMonthPostInfo(userId, month);
        return new BaseResponse<>(userMonthPostInfo);
    }

    @PatchMapping("/update/{postId}")
    public BaseResponse<String> update(
            @PathVariable("postId") Long postId,
            @RequestPart PostUpdateDto postUpdateDto,
            @RequestPart(required = false) List<MultipartFile> addImgs,
            @RequestPart(required = false) List<MultipartFile> addVideos) throws Exception {
        postService.update(postId, postUpdateDto, addImgs, addVideos);
        return new BaseResponse<>("?????? ????????? ?????????????????????.");
    }

    @DeleteMapping("/delete/{postId}")
    public BaseResponse<String> delete(@PathVariable("postId") Long postId) throws Exception {
        postService.delete(postId);
        return new BaseResponse<>("?????? ????????? ?????????????????????.");
    }
}
