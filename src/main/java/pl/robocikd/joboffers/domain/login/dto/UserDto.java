package pl.robocikd.joboffers.domain.login.dto;

import lombok.Builder;

@Builder
public record UserDto(String id, String password, String username) {
}
