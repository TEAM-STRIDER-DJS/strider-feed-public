//package com.strider.feed.service;
//
//import com.strider.feed.model.entity.FeedMedia;
//import com.strider.feed.model.entity.FeedPost;
//import com.strider.feed.repository.FeedPostRepository;
//import com.strider.strider_common_lib.error.StriderErrorCodes;
//import com.strider.strider_common_lib.exception.StriderException;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import com.strider.feed.repository.*;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class SharePageService {
//    private final FeedPostRepository postRepository;
//    private final FeedMediaRepository mediaRepository;
//
//    @Value("${strider.share.base-url:https://www.strider.r-e.kr}")
//    private String shareBaseUrl;
//
//    @Value("${strider.share.default-image-url:https://www.strider.r-e.kr/static/share-default.png}")
//    private String defaultImageUrl;
//
//    public String buildFeedOgHtml(String feedId) {
//        FeedPost post = postRepository.findByFeedPostIdAndIsDeletedFalse(feedId)
//                        .orElseThrow(() -> new StriderException(StriderErrorCodes.NOT_FOUND));
//        List<FeedMedia> mediaList = mediaRepository.findByFeedPostIdAndIsDeletedFalseOrderBySequence(feedId);
//
//        String url = shareBaseUrl + "/p/" + feedId;
//
//        String title = "Strider 피드";
//        String description = "Strider에서 피드를 확인해보세요.";
//        String imageUrl = defaultImageUrl;
//        if(!mediaList.isEmpty()){
//            imageUrl = mediaList.get(0);
//        }
//
//    }
//}
