package com.graduation.forumservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for pagination metadata in notification list response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginationDTO {
    private Integer currentPage;
    private Boolean hasMore;
}
