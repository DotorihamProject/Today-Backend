package com.today.todayproject.domain.post.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostSaveResponseDto {

    private Long postId;
    private List<Long> postQuestionId;
}
