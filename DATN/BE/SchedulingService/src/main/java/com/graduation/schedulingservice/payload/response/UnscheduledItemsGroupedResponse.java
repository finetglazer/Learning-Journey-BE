package com.graduation.schedulingservice.payload.response;

import lombok.Data;
import java.util.List;

@Data
public class UnscheduledItemsGroupedResponse {
    private List<MonthGroupDTO> monthGroups;
}