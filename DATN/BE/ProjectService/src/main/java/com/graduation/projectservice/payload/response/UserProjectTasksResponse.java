package com.graduation.projectservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProjectTasksResponse {
    private List<ProjectGroupDTO> projects;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectGroupDTO {
        private Long projectId;
        private String projectName;
        private List<UserTaskItemDTO> tasks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserTaskItemDTO {
        private Long pmTaskId;
        private String name;
        private LocalDate deadline;
        private boolean isOverdue;
    }
}