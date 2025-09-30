package com.graduation.userservice.controller;

import com.graduation.userservice.payload.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/users")
@RestController
@RequiredArgsConstructor
public class TestController {

    // Testing endpoint
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(new BaseResponse<>(1, "Test successfully", null));
    }
}
