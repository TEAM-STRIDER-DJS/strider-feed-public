package com.strider.feed.client;

import com.strider.feed.model.request.UserProfileListRequest;
import com.strider.feed.model.request.UserProfileRequest;
import com.strider.feed.model.response.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "userProfileClient", url = "http://strider-user-profile:8001")
// @FeignClient(name = "userProfileClient", url = "http://localhost:8001")
public interface UserProfileClient {
    // TODO: param이 request 객체를 쓰는 것과 기본 type이 혼용되어있음. 여러개면 request 객체, 한개면 기본 type을 쓰는 게 좋을듯
    @PostMapping(value = "/api/v1/user/profile/find")
    Envelope<UserProfileResponse> getUserProfile(@RequestBody UserProfileRequest request);

    @PostMapping(value = "/api/v1/user/profile/find/batch")
    Envelope<List<SimpleUserProfileResponse>> getSimpleUserProfileList(@RequestBody UserProfileListRequest request);

    @GetMapping(value = "/api/v1/user/profile/{id}/following")
    Envelope<List<UserFollowResponse>> getFollowingIdList(@PathVariable("id") String id);

    @GetMapping("/api/v1/user/profile/{id}/followers")
    Envelope<List<UserFollowResponse>> getFollowerIdList(@PathVariable("id") String id);

    @GetMapping(value = "/api/v1/user/profile/{userId}/visibility")
    Envelope<Boolean> isUserPublic(@PathVariable String userId);
}