package pl.robocikd.joboffers.domain.login;

import lombok.Builder;

@Builder
public record User(String id, String username, String password) {
}
