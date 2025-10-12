package com.graduation.userservice.controller;

import com.graduation.userservice.model.UserConstraints;
import com.graduation.userservice.payload.response.UserConstraintsDTO;
import com.graduation.userservice.repository.UserConstraintsRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/users/{userId}/constraints")
@RequiredArgsConstructor
public class UserConstraintsInternalController {

    private final UserConstraintsRepository userConstraintsRepository;
    private final ModelMapper modelMapper; // Add a ModelMapper bean to your project

    @GetMapping
    public ResponseEntity<UserConstraintsDTO> getUserConstraints(@PathVariable Long userId) {
        return userConstraintsRepository.findByUserId(userId)
                .map(constraints -> {
                    // Map the entity to the DTO
                    UserConstraintsDTO dto = modelMapper.map(constraints, UserConstraintsDTO.class);
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}