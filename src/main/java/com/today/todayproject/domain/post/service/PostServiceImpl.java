package com.today.todayproject.domain.post.service;

import com.today.todayproject.domain.crop.Crop;
import com.today.todayproject.domain.crop.repository.CropRepository;
import com.today.todayproject.domain.friend.Friend;
import com.today.todayproject.domain.friend.repository.FriendRepository;
import com.today.todayproject.domain.growncrop.GrownCrop;
import com.today.todayproject.domain.growncrop.repository.GrownCropRepository;
import com.today.todayproject.domain.post.Post;
import com.today.todayproject.domain.post.dto.*;
import com.today.todayproject.domain.post.imgurl.PostImgUrl;
import com.today.todayproject.domain.post.imgurl.repository.PostImgUrlRepository;
import com.today.todayproject.domain.post.question.PostQuestion;
import com.today.todayproject.domain.post.question.dto.PostQuestionDto;
import com.today.todayproject.domain.post.question.dto.PostQuestionUpdateDto;
import com.today.todayproject.domain.post.question.repository.PostQuestionRepository;
import com.today.todayproject.domain.post.repository.PostRepository;
import com.today.todayproject.domain.post.video.PostVideoUrl;
import com.today.todayproject.domain.post.video.repository.PostVideoUrlRepository;
import com.today.todayproject.domain.user.User;
import com.today.todayproject.domain.user.repository.UserRepository;
import com.today.todayproject.global.BaseException;
import com.today.todayproject.global.BaseResponseStatus;
import com.today.todayproject.global.s3.service.S3UploadService;
import com.today.todayproject.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService{

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostQuestionRepository postQuestionRepository;
    private final S3UploadService s3UploadService;
    private final CropRepository cropRepository;
    private final GrownCropRepository grownCropRepository;
    private final PostImgUrlRepository postImgUrlRepository;
    private final PostVideoUrlRepository postVideoUrlRepository;
    private final FriendRepository friendRepository;

    private static final int CROP_HARVEST_WRITE_COUNT = 7;

    //TODO : ????????? ????????? ????????? ?????? ??????????????? ?????? -> ??????
    @Override
    public PostSaveResponseDto save(PostSaveDto postSaveDto, List<MultipartFile> uploadImgs, List<MultipartFile> uploadVideos) throws Exception {
        User loginUser = userRepository.findByEmail(SecurityUtil.getLoginUserEmail())
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_LOGIN_USER));

//        if (loginUser.getCanWritePost() == false) {
//            throw new BaseException(BaseResponseStatus.POST_CAN_WRITE_ONLY_ONCE_A_DAY);
//        }

        Post post = Post.builder()
                .todayFeeling(postSaveDto.getTodayFeeling())
                .writer(loginUser)
                .canPublicAccess(postSaveDto.getCanPublicAccess())
                .build();

        List<PostQuestionDto> postQuestions = postSaveDto.getPostQuestions();
        // Questions(?????????) ????????? content, imgUrl, videoUrl ??????
        postQuestions.stream().forEach(postQuestionDto -> {
            String question = postQuestionDto.getQuestion();
            String content = postQuestionDto.getContent();

            PostQuestion postQuestion = PostQuestion.builder()
                    .question(question)
                    .content(content)
                    .build();

            postQuestion.confirmPost(post);

            int addImgCount = postQuestionDto.getImgCount();
            int addVideoCount = postQuestionDto.getVideoCount();

            if(addImgCount != 0) {
                addImgsAndConfirmPost(uploadImgs, post, postQuestion, addImgCount);
            }

            if(addVideoCount != 0) {
                addVideosAndConfirmPost(uploadVideos, post, postQuestion, addVideoCount);
            }
        });

        // before??? ?????? ?????? ?????? -> .getPostWriteCount, .getThisMonthHarvestCount??? if ????????? ???????????????,
        // ????????? ????????? ?????? ????????? ??? postWriteCount??? ??????, .getThisMonthHarvestCount??? ??????????????? ????????????,
        // ????????? ??? ????????? ???????????? ?????? ?????? ????????? ?????????
        int beforeIncreasePostWriteCount = loginUser.getPostWriteCount();

        if (beforeIncreasePostWriteCount == 0) {
            Random random = new Random();
            Crop crop = Crop.builder()
                    .cropNumber(random.nextInt(10) + 1)
                    .createdMonth(LocalDateTime.now().getMonthValue())
                    .isHarvested(false)
                    .build();

            crop.confirmUser(loginUser);
            loginUser.addPostWriteCount();
            crop.updateCropStatus(loginUser.getPostWriteCount());
            post.confirmCrop(crop);
            cropRepository.save(crop);
        }
        if (beforeIncreasePostWriteCount != 0) {
            Crop findCrop = cropRepository.findByUserIdAndIsHarvested(loginUser.getId(), false)
                    .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_CROP));
            loginUser.addPostWriteCount();
            findCrop.updateCropStatus(loginUser.getPostWriteCount());
            post.confirmCrop(findCrop);

            int beforeThisMonthHarvestCount = loginUser.getThisMonthHarvestCount();

            // thieMonthHarvestCount increase ??? 4??? ?????? ?????? ?????? ?????? (?????? ?????? ?????? = -1)
            if (loginUser.getPostWriteCount() == CROP_HARVEST_WRITE_COUNT && beforeThisMonthHarvestCount == 3) {
                GrownCrop goldGrownCrop = GrownCrop.builder()
                        .cropNumber(-1)
                        .harvestedMonth(LocalDateTime.now().getMonthValue())
                        .build();

                goldGrownCrop.confirmUser(loginUser);
                grownCropRepository.save(goldGrownCrop);
                loginUser.increaseThisMonthHarvestCount();
                findCrop.harvest();
                loginUser.initPostWriteCount();
            }

            //  thieMonthHarvestCount increase ??? 4??? ????????? ?????? ?????? ??????
            if (loginUser.getPostWriteCount() == CROP_HARVEST_WRITE_COUNT && beforeThisMonthHarvestCount != 3) {
                GrownCrop grownCrop = GrownCrop.builder()
                        .cropNumber(findCrop.getCropNumber())
                        .harvestedMonth(LocalDateTime.now().getMonthValue())
                        .build();

                grownCrop.confirmUser(loginUser);
                grownCropRepository.save(grownCrop);
                loginUser.increaseThisMonthHarvestCount();
                findCrop.harvest();
                loginUser.initPostWriteCount();
            }
        }

        loginUser.updateRecentFeeling(postSaveDto.getTodayFeeling());
        loginUser.writePost();
        postRepository.save(post);
        List<Long> postQuestionIds = post.getPostQuestions().stream()
                .map(postQuestion -> postQuestion.getId())
                .collect(Collectors.toList());
        return new PostSaveResponseDto(post.getId(), postQuestionIds);
    }

    private void addVideosAndConfirmPost(List<MultipartFile> uploadVideos, Post post, PostQuestion postQuestion, int addVideoCount) {
        List<PostVideoUrl> postVideoUrls = addVideos(uploadVideos, addVideoCount);
        for (PostVideoUrl postVideoUrl : postVideoUrls) {
            confirmVideoUrlPostAndPostQuestion(post, postQuestion, postVideoUrl);
        }
    }

    private void addImgsAndConfirmPost(List<MultipartFile> uploadImgs, Post post, PostQuestion postQuestion, int addImgCount) {
        List<PostImgUrl> postImgUrls = addImgs(uploadImgs, addImgCount);
        for (PostImgUrl postImgUrl : postImgUrls) {
            confirmImgUrlPostAndPostQuestion(post, postQuestion, postImgUrl);
        }
    }

    @Override
    public PostInfoDto getPostInfo(Long postId, Long userId) throws Exception {
        User loginUser = userRepository.findByEmail(SecurityUtil.getLoginUserEmail())
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_LOGIN_USER));

        User findUser = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_USER));

        Post findPost = postRepository.findById(postId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_POST));

        validateFindUser(loginUser, findUser, findPost);

        return new PostInfoDto(findPost);
    }

    private void validateFindUser(User loginUser, User findUser, Post findPost) throws BaseException {
        // ?????? ????????? ????????? ????????? ???????????? ????????? ??????????????? ?????? ?????? ?????? X
//        if (findPost.getCanPublicAccess() == true) {
//            checkFriendLoginUserAndFindUser(loginUser, findUser);
//        }
        if (findPost.getCanPublicAccess() == false) {
            checkFindUserEqualLoginUser(loginUser, findUser);
        }
    }

//    private void checkFriendLoginUserAndFindUser(User loginUser, User findUser) throws BaseException {
//        boolean isLoginUserFriend = friendRepository.existsByFriendOwnerIdAndFriendAndAreWeFriend(
//                loginUser.getId(), findUser, true);
//        boolean isFindUserFriend = friendRepository.existsByFriendOwnerIdAndFriendAndAreWeFriend(
//                findUser.getId(), loginUser, true);
//        if (!(isLoginUserFriend && isFindUserFriend)) {
//            throw new BaseException(BaseResponseStatus.CANNOT_SEE_POST_NOT_FRIEND_USER);
//        }
//    }

    private void checkFindUserEqualLoginUser(User loginUser, User findUser) throws BaseException {
        if (!loginUser.getId().equals(findUser.getId())) {
            throw new BaseException(BaseResponseStatus.CANNOT_SEE_POST_NOT_LOGIN_USER);
        }
    }

    @Override
    public PostGetMonthInfoDto getUserMonthPostInfo(Long userId, int month) {
        List<Post> findPosts = postRepository.getPostByUserIdAndMonth(userId, month);

        return new PostGetMonthInfoDto(findPosts);
    }

    @Override
    public void update(Long postId, PostUpdateDto postUpdateDto,
                       List<MultipartFile> addImgs, List<MultipartFile> addVideos) throws Exception {
        Post findPost = postRepository.findById(postId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_POST));

        for (PostQuestionUpdateDto postQuestionUpdateDto : postUpdateDto.getPostQuestions()) {
            PostQuestion findPostQuestion = postQuestionRepository.findById(postQuestionUpdateDto.getQuestionId())
                    .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_POST_QUESTION));
            findPostQuestion.updateContent(postQuestionUpdateDto.getContent());

            // ??????
            deleteImgs(findPost, findPostQuestion, postQuestionUpdateDto.getDeleteImgUrlId());
            deleteVideos(findPost, findPostQuestion, postQuestionUpdateDto.getDeleteVideoUrlId());

            int addImgCount = postQuestionUpdateDto.getAddImgCount();
            int addVideoCount = postQuestionUpdateDto.getAddVideoCount();

            // ??????
            if (addImgCount != 0) {
                addImgsAndConfirmPost(addImgs, findPost, findPostQuestion, addImgCount);
            }

            if (addVideoCount != 0) {
                addVideosAndConfirmPost(addVideos, findPost, findPostQuestion, addVideoCount);
            }
        }
        findPost.updateTodayFeeling(postUpdateDto.getTodayFeeling());
        findPost.updateCanPublicAccess(postUpdateDto.getCanPublicAccess());
    }

    private void confirmImgUrlPostAndPostQuestion(Post findPost, PostQuestion findPostQuestion,
                                                  PostImgUrl postImgUrl) {
        postImgUrl.confirmPost(findPost);
        postImgUrl.confirmPostQuestion(findPostQuestion);
    }

    private void confirmVideoUrlPostAndPostQuestion(Post findPost, PostQuestion findPostQuestion,
                                                    PostVideoUrl postVideoUrl) {
        postVideoUrl.confirmPost(findPost);
        postVideoUrl.confirmPostQuestion(findPostQuestion);
    }

    private void deleteImgs(Post post, PostQuestion postQuestion, List<Long> deleteImgUrlIds) throws BaseException {
        for (Long deleteImgUrlId : deleteImgUrlIds) {
            PostImgUrl findPostImgUrl = postImgUrlRepository.findById(deleteImgUrlId)
                    .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_IMG));
            postQuestion.removeImgUrl(findPostImgUrl);
            post.removeImgUrl(findPostImgUrl);
            s3UploadService.deleteOriginalFile(findPostImgUrl.getImgUrl());
        }
    }

    private void deleteVideos(Post post, PostQuestion postQuestion, List<Long> deleteVideoIds) throws BaseException {
        for (Long deleteVideoUrlId : deleteVideoIds) {
            PostVideoUrl findPostVideoUrl = postVideoUrlRepository.findById(deleteVideoUrlId)
                    .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_VIDEO));
            postQuestion.removeVideoUrl(findPostVideoUrl);
            post.removeVideoUrl(findPostVideoUrl);
            s3UploadService.deleteOriginalFile(findPostVideoUrl.getVideoUrl());
        }
    }

    private List<PostImgUrl> addImgs(List<MultipartFile> addImgs, int addImgCount) {
        List<PostImgUrl> addPostImgUrls = new ArrayList<>();
        List<String> addImgUrls = s3UploadService.uploadFiles(addImgs);
        for (int addIndex = 0; addIndex < addImgCount; addIndex++) {
            PostImgUrl postImgUrl = PostImgUrl.builder().imgUrl(addImgUrls.get(0)).build();
            addImgUrls.remove(0);
            addPostImgUrls.add(postImgUrl);
            addImgs.remove(0);
        }
        return addPostImgUrls;
    }

    private List<PostVideoUrl> addVideos(List<MultipartFile> addVideos, int addVideoCount) {
        List<PostVideoUrl> addPostVideoUrls = new ArrayList<>();
        List<String> addVideoUrls = s3UploadService.uploadFiles(addVideos);
        for (int addIndex = 0; addIndex < addVideoCount; addIndex++) {
            PostVideoUrl postVideoUrl = PostVideoUrl.builder().videoUrl(addVideoUrls.get(0)).build();
            addVideoUrls.remove(0);
            addPostVideoUrls.add(postVideoUrl);
            addVideos.remove(0);
        }
        return addPostVideoUrls;
    }

    @Override
    public void delete(Long postId) throws Exception {
        Post deletePost = postRepository.findById(postId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_POST));


        User loginUser = userRepository.findByEmail(SecurityUtil.getLoginUserEmail())
                .orElseThrow(() -> new BaseException(BaseResponseStatus.NOT_FOUND_LOGIN_USER));

        Crop recentCrop = cropRepository.findTopByUserIdAndCreatedMonthOrderByCreatedDateDesc(loginUser.getId(),
                        LocalDateTime.now().getMonthValue())
                .orElse(null);

        Crop cropOfDeletePost = deletePost.getCrop();

        userInitPostWriteCount(deletePost, loginUser);

        // ?????? ??? ?????? ?????? ????????? ????????? ?????????(?????? ??? ????????? ??????), ????????? ????????? Post??? ?????? ?????? ???????????? ?????? ???????????? Post??? ??????
        // ?????? ??? ???????????? ?????? ??? ?????? ???????????? ????????? ?????? ??????, ?????? ?????? ?????? Post??? ??????
        if (recentCrop == null || !recentCrop.equals(cropOfDeletePost)) {
            deleteImgsAndVideos(deletePost);
            postRepository.delete(deletePost);
        }

        // ?????? ??? ???????????? ?????? ????????? ????????? ??????
        if (recentCrop.equals(cropOfDeletePost)) {
            deleteImgsAndVideos(deletePost);
            postRepository.delete(deletePost);

            // postWriteCount??? 0????????? ?????? ????????? ???(????????? ?????? ?????? ????????? ????????? ?????? ??????)????????? ?????? ???????????? GrownCrop(????????? ??????)??? ??????
            if (loginUser.getPostWriteCount() == 0) {
                loginUser.rollbackPostWriteCountBeforeHarvest();
                cropOfDeletePost.updateCropStatus(loginUser.getPostWriteCount());
                grownCropRepository.deleteByUserIdOrderByCreatedDateDesc(loginUser.getId());
            }
            if (loginUser.getPostWriteCount() == 1) {
                loginUser.minusPostWriteCount();
                cropRepository.delete(cropOfDeletePost);
            }
            if (loginUser.getPostWriteCount() != 0 && loginUser.getPostWriteCount() != 1) {
                loginUser.minusPostWriteCount();
                cropOfDeletePost.updateCropStatus(loginUser.getPostWriteCount());
            }
        }
    }

    private void userInitPostWriteCount(Post deletePost, User loginUser) {
        int nowHour = LocalDateTime.now().getHour();
        int nowDay = LocalDateTime.now().getDayOfMonth();
        int deletePostHour = deletePost.getCreatedDate().getHour();
        int deletePostDay = deletePost.getCreatedDate().getDayOfMonth();
        // ?????? ????????? ?????????
        if (nowHour <= 3) {
            if (deletePostDay == nowDay || (deletePostDay == nowDay -1 && deletePostHour > 3)) {
                loginUser.initCanWritePost();
            }
        }
        if (nowHour > 3) {
            if (deletePostDay == nowDay) {
                loginUser.initCanWritePost();
            }
        }
    }

    private void deleteImgsAndVideos(Post deletePost) {
        deletePost.getPostImgUrls().stream()
                        .forEach(postImgUrl -> s3UploadService.deleteOriginalFile(postImgUrl.getImgUrl()));

        deletePost.getPostVideoUrls().stream()
                        .forEach(postVideoUrl -> s3UploadService.deleteOriginalFile(postVideoUrl.getVideoUrl()));
    }

    @Override
    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Seoul")
    public void initUserCanWritePost() {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            user.initCanWritePost();
        }
    }
}
