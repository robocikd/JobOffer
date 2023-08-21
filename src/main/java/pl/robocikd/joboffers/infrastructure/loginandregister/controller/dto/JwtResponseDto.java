package pl.robocikd.joboffers.infrastructure.loginandregister.controller.dto;

import lombok.Builder;

@Builder
public record JwtResponseDto(
        String username,
        String token
) {
}
