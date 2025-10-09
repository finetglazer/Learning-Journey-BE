package com.graduation.userservice.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTimezoneRequest {

    @NotBlank(message = "Timezone cannot be blank")
    private String timezone;
}