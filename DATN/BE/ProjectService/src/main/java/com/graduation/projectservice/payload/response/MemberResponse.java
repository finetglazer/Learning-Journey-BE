package com.graduation.projectservice.payload.response;

import com.graduation.projectservice.model.ProjectMembershipRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponse {
    private Long userId;
    private String name;
    private String avatarUrl;
    private String email;
    private ProjectMembershipRole role;
    private String customRoleName;
}
