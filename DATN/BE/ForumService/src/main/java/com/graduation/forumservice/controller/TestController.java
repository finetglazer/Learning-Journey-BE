package com.graduation.forumservice.controller;

import com.graduation.forumservice.constant.Constant;
import com.graduation.forumservice.payload.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/forum/test")
@RequiredArgsConstructor
public class TestController {

    @GetMapping("/health")
    public BaseResponse<String> healthCheck() {
        log.info(Constant.LOG_TEST_HEALTH_REQUEST);
        return new BaseResponse<>(
                Constant.SUCCESS_STATUS,
                Constant.SERVICE_RUNNING,
                "OK");
    }

    @GetMapping("/info")
    public BaseResponse<Map<String, Object>> getServiceInfo() {
        log.info(Constant.LOG_TEST_INFO_REQUEST);

        Map<String, Object> info = new HashMap<>();
        info.put("serviceName", "ForumService");
        info.put("version", "0.0.1-SNAPSHOT");
        info.put("status", "running");
        info.put("timestamp", LocalDateTime.now());
        info.put("description", "Forum Service for managing forums, posts, and comments");

        return new BaseResponse<>(
                Constant.SUCCESS_STATUS,
                Constant.SERVICE_INFO_RETRIEVED,
                info);
    }
}
