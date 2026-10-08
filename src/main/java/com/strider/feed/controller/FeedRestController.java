package com.strider.feed.controller;

import com.strider.feed.model.request.*;
import com.strider.feed.model.response.*;
import com.strider.feed.model.TargetType;
import com.strider.feed.service.FeedSaveService;
import com.strider.feed.service.FeedService;
import com.strider.strider_common_lib.response.StriderResponse;
import com.strider.feed.service.FeedCommentService;
import com.strider.strider_common_lib.utils.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/feed")
public class FeedRestController {
    private final FeedService feedService;
    private final FeedCommentService feedCommentService;
    private final FeedSaveService feedSaveService;
    private final TokenUtils tokenUtils;

    // ====================== 피드 ======================
    @PostMapping(value = "")
    public ResponseEntity<StriderResponse<FeedPostResponse>> createFeed(@RequestHeader HttpHeaders headers, @RequestBody FeedPostRequest request){
        String userId = tokenUtils.getUidFrom(headers);

        FeedPostResponse response = feedService.createFeed(userId, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, FeedPostResponse.class));
    }

    @GetMapping(value = "/{feedId}")
    public ResponseEntity<?> getFeed(@RequestHeader HttpHeaders headers, @PathVariable("feedId") String feedId){
        String userId = tokenUtils.getUidFrom(headers);

        FeedPostResponse response = feedService.findFeed(userId, feedId); // TODO: 추후 userId는 토큰에서 가져와야 함.

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, Object.class));
    }

    @GetMapping(value = "/follow")
    public ResponseEntity<StriderResponse<?>> getFollowerFeeds(@RequestHeader HttpHeaders headers, @ModelAttribute CursorQuery query){
        String userId = tokenUtils.getUidFrom(headers);

        int size = query.size() == null ? 5 : query.size();

        UserFeedQuery request = new UserFeedQuery(userId, null, size, query.cursorCreatedAt(), query.cursorId());
        List<FeedPostResponse> response = feedService.findFollowerFeeds(headers, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, List.class));
    }

    @GetMapping(value = "/user/{feedUserId}")
    public ResponseEntity<StriderResponse<?>> getUserFeeds(@RequestHeader HttpHeaders headers,
                                                           @ModelAttribute CursorQuery query,
                                                           @PathVariable String feedUserId) {
        String userId = tokenUtils.getUidFrom(headers);

        int size = query.size() == null ? 5 : query.size();

        UserFeedQuery request = new UserFeedQuery(userId, feedUserId, size, query.cursorCreatedAt(), query.cursorId());
        List<FeedPostResponse> response = feedService.findUserFeeds(headers, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, List.class));
    }

    @GetMapping(value = "/user/{feedUserId}/count")
    public ResponseEntity<StriderResponse<Long>> getUserFeedCount(@PathVariable String feedUserId) {
        long count = feedService.countUserFeeds(feedUserId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(count, Long.class));
    }

    // 알림 서비스용 - 게시물 썸네일 배치 조회 (좋아요/댓글 알림 → resourceId가 feedPostId인 경우)
    @PostMapping(value = "/thumbnails/batch")
    public ResponseEntity<StriderResponse<?>> getFeedThumbnails(@RequestBody FeedThumbnailBatchRequest request) {
        List<FeedThumbnailResponse> response = feedService.getThumbnailsByFeedPostIds(request.feedPostIds());

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, List.class));
    }

    // 알림 서비스용 - 댓글 썸네일 배치 조회 (댓글 좋아요 알림 → resourceId가 commentId인 경우)
    @PostMapping(value = "/comment/thumbnails/batch")
    public ResponseEntity<StriderResponse<?>> getCommentThumbnails(@RequestBody CommentThumbnailBatchRequest request) {
        List<CommentThumbnailResponse> response = feedService.getThumbnailsByCommentIds(request.commentIds());

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, List.class));
    }

    @GetMapping(value = "/me")
    public ResponseEntity<StriderResponse<?>> getMyFeeds(@RequestHeader HttpHeaders headers, @ModelAttribute CursorQuery query) {
        String userId = tokenUtils.getUidFrom(headers);

        int size = query.size() == null ? 5 : query.size();

        UserFeedQuery request = new UserFeedQuery(userId, null, size, query.cursorCreatedAt(), query.cursorId());
        List<FeedPostResponse> response = feedService.findMyFeeds(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, List.class));
    }

    @PostMapping(value = "/{feedId}/like")
    public ResponseEntity<StriderResponse<FeedLikeResponse>> likeFeed(@RequestHeader HttpHeaders headers, @PathVariable("feedId") String feedId) {
        String userId = tokenUtils.getUidFrom(headers);

        FeedLikeRequest request = new FeedLikeRequest(userId, TargetType.FEED, feedId); // TODO:  userId는 토큰에서 가져와야 함.
        FeedLikeResponse response = feedService.likeFeed(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, FeedLikeResponse.class));
    }

    @DeleteMapping(value = "/{feedId}/like")
    public ResponseEntity<StriderResponse<FeedLikeResponse>> unlikeFeed(@RequestHeader HttpHeaders headers, @PathVariable("feedId") String feedId) {
        String userId = tokenUtils.getUidFrom(headers);

        FeedLikeRequest request = new FeedLikeRequest(userId, TargetType.FEED, feedId); // TODO:  userId는 토큰에서 가져와야 함.
        FeedLikeResponse response = feedService.unLikeFeed(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, FeedLikeResponse.class));
    }

    @GetMapping(value = "/{feedId}/like/users")
    public ResponseEntity<StriderResponse<?>> getFeedLikeUsers(@RequestHeader HttpHeaders headers,
                                                               @ModelAttribute CursorQuery query,
                                                               @PathVariable("feedId") String feedId){
        String userId = tokenUtils.getUidFrom(headers);

        int size = query.size() == null ? 20 : query.size();

        FeedLikeUserProfileQuery request = new FeedLikeUserProfileQuery(feedId, userId, size, query.cursorCreatedAt(), query.cursorId());
        List<SimpleUserFollowResponse> response = feedService.findFeedLikeUserProfileList(headers, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, List.class));
    }

    @PostMapping(value = "/{feedId}/save")
    public ResponseEntity<StriderResponse<FeedSaveResponse>> saveFeed(@RequestHeader HttpHeaders headers, @PathVariable("feedId") String feedId) {
        String userId = tokenUtils.getUidFrom(headers);

        FeedSaveRequest request = new FeedSaveRequest(userId, feedId); // TODO: 추후 userId는 토큰에서 가져와야 함.
        FeedSaveResponse response = feedService.saveFeed(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, FeedSaveResponse.class));
    }

    @DeleteMapping(value = "/{feedId}/save")
    public ResponseEntity<StriderResponse<FeedSaveResponse>> unsaveFeed(@RequestHeader HttpHeaders headers, @PathVariable("feedId") String feedId) {
        String userId = tokenUtils.getUidFrom(headers);

        FeedSaveRequest request = new FeedSaveRequest(userId, feedId); // TODO: 추후 userId는 토큰에서 가져와야 함.
        FeedSaveResponse response = feedService.unSaveFeed(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, FeedSaveResponse.class));
    }

//    @GetMapping(value = "/media")
//    public ResponseEntity<?> getFeedMedia(){
//        List<FeedMediaResponse> response = feedService.findFeedMedia();
//
//        return ResponseEntity.status(HttpStatus.OK)
//                .body(StriderResponse.responseBuilder(response, List.class));
//    }

//    @GetMapping("/{feedId}/share-url")
//    public ResponseEntity<?> createShareUrl(@RequestHeader HttpHeaders headers, @PathVariable("feedId") String feedId){
//        String BASE_URL = "https://www.strider.r-e.kr";
//        String shareUrl = BASE_URL + "/api/v1/feed/" + feedId + "/share";
//        FeedShareUrlResponse response = new FeedShareUrlResponse(feedId, shareUrl);
//
//        return ResponseEntity.status(HttpStatus.OK)
//                .body(StriderResponse.responseBuilder(response, FeedShareUrlResponse.class));
//    }

//    @GetMapping("/{feedId}/share")
//    public ResponseEntity<String> openAppOrStore(@PathVariable("feedId") String feedId, @RequestHeader(value = "User-Agent", required = false) String ua){
//        String appLink = "strider://feed/" + feedId;
//        String iosStore = "https://apps.apple.com/app/id1234567890";
//        String andStore = "https://play.google.com/store/apps/details?id=com.strider.app";
//
//        String uaLower = ua == null ? "" : ua.toLowerCase();
//        String storeUrl = uaLower.contains("android") ? andStore : iosStore;
//
//        String html = """
//        <!doctype html>
//        <meta charset="utf-8">
//        <script>
//          (function(){
//            window.location = '%s'; // 앱 열기 시도
//            setTimeout(function(){ window.location = '%s'; }, 1500); // 실패 시 스토어 이동
//          })();
//        </script>
//        """.formatted(appLink, storeUrl);
//
//        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
//    }

    // 피드의 댓글 리스트 조회
    @GetMapping(value = "/{feedId}/comment")
    public ResponseEntity<StriderResponse<?>> getFeedComments(@RequestHeader HttpHeaders headers,
                                                              @ModelAttribute CursorQuery query,
                                                              @PathVariable("feedId") String feedId){
        String userId = tokenUtils.getUidFrom(headers);

        int size = query.size() == null ? 30 : query.size();

        FeedCommentQuery request = new FeedCommentQuery(userId, feedId, size, query.cursorCreatedAt(), query.cursorId());
        List<FeedCommentResponse> response = feedCommentService.findCommentsByFeed(headers, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, List.class));
    }

    // 저장된 피드 전체 조회
    @GetMapping("/saved")
    public ResponseEntity<StriderResponse<?>> getSavedFeeds(@RequestHeader HttpHeaders headers) {
        String userId = tokenUtils.getUidFrom(headers);

        List<FeedPostResponse> response = feedSaveService.getSavedFeeds(userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, List.class));
    }

    // 저장된 피드 썸네일 조회
    @GetMapping("/saved/thumbnails")
    public ResponseEntity<StriderResponse<?>> getSavedFeedThumbnails(@RequestHeader HttpHeaders headers) {
        String userId = tokenUtils.getUidFrom(headers);

        List<SavedFeedThumbnailResponse> response = feedSaveService.getSavedFeedThumbnails(userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, List.class));
    }
}
