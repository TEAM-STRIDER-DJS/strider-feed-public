package com.strider.feed.controller;

import com.strider.feed.model.TargetType;
import com.strider.feed.model.request.*;
import com.strider.feed.model.response.FeedCommentResponse;
import com.strider.feed.model.response.FeedCommentResult;
import com.strider.feed.model.response.FeedLikeResponse;
import com.strider.feed.service.FeedCommentService;
import com.strider.strider_common_lib.response.StriderResponse;
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
@RequestMapping("/api/v1/feed/comment")
public class CommentRestController {
    private final FeedCommentService feedCommentService;
    private final TokenUtils tokenUtils;

    @PostMapping(value = "")
    public ResponseEntity<StriderResponse<FeedCommentResult>> createComment(@RequestHeader HttpHeaders headers, @RequestBody FeedCommentRequest request){
        String userId = tokenUtils.getUidFrom(headers);

        FeedCommentResult response = feedCommentService.createComment(userId, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, FeedCommentResult.class));
    }

    @GetMapping(value = "/{commentId}/reply")
    public ResponseEntity<StriderResponse<?>> getReplies(@RequestHeader HttpHeaders headers, @ModelAttribute CursorQuery query, @PathVariable("commentId") String commentId){
        String userId = tokenUtils.getUidFrom(headers);

        int size = query.size() == null ? 30 : query.size();

        CommentReplyQuery request = new CommentReplyQuery(userId, commentId, size, query.cursorCreatedAt(), query.cursorId());

        List<FeedCommentResponse> response = feedCommentService.findRepliesByComment(headers, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, List.class));
    }

    @DeleteMapping(value = "/{commentId}")
    public ResponseEntity<StriderResponse<Void>> deleteComment(@RequestHeader HttpHeaders headers, @PathVariable("commentId") String commentId){
        String userId = tokenUtils.getUidFrom(headers);

        feedCommentService.deleteComment(userId, commentId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(null, Void.class));
    }

    @PostMapping(value = "/{commentId}/like")
    public ResponseEntity<StriderResponse<FeedLikeResponse>> likeComment(@RequestHeader HttpHeaders headers, @PathVariable("commentId") String commentId) {
        String userId = tokenUtils.getUidFrom(headers);

        FeedLikeRequest request = new FeedLikeRequest(userId, TargetType.COMMENT, commentId);
        FeedLikeResponse response = feedCommentService.likeComment(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, FeedLikeResponse.class));
    }

    @DeleteMapping(value = "/{commentId}/like")
    public ResponseEntity<StriderResponse<FeedLikeResponse>> unlikeComment(@RequestHeader HttpHeaders headers, @PathVariable("commentId") String commentId) {
        String userId = tokenUtils.getUidFrom(headers);

        FeedLikeRequest request = new FeedLikeRequest(userId, TargetType.COMMENT, commentId);
        FeedLikeResponse response = feedCommentService.unlikeComment(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(StriderResponse.responseBuilder(response, FeedLikeResponse.class));
    }
}
