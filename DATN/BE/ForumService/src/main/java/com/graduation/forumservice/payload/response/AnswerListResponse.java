package com.graduation.forumservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerListResponse {
    private List<AnswerDTO> posts;
    private PaginationDTO pagination;
}