//package com.strider.feed.controller;
//
//import com.strider.feed.model.response.FeedPostResponse;
//import com.strider.feed.service.SharePageService;
//import com.strider.strider_common_lib.response.StriderResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@Slf4j
//@RequiredArgsConstructor
//@RequestMapping("")
//public class ShareRestController {
////    // 공유된 링크를 눌렀을 때
////    @GetMapping(value = "/p/{feedId}", produces = "text/html; charset=UTF-8")
////    public ResponseEntity<Void> shareLanding(@PathVariable("feedId") String feedId){
////        String html = """
////            <!doctype html>
////            <html lang="ko">
////            <head>
////                <meta charset="utf-8">
////                <meta name="viewport" content="width=divice-width, initial-scale=1" />
////                <title>Strider</title>
////            </head>
////            <body>
////                <p>Strider로 이동중...</p>
////                <script>
////                     (function () {
////                        var feedPostId = "%s";
////
////                        var appSchemeUrl = "strider//
////                     })
////                </script>
////            </body>
////        """;
////    }
//
////    private final SharePageService sharePageService;
////
////    @GetMapping(value = "/p/{feedId}", produces = "text/html; charset=UTF-8")
////    public ResponseEntity<String> sharePage(@PathVariable("feedId") String feedId){
////        String html = sharePageService.buildFeedOgHtml(feedId);
////        return ResponseEntity
////                .ok()
////                .contentType(MediaType.TEXT_HTML)
////                .body(html);
////    }
//}
