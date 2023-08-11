package pl.robocikd.joboffers.domain.login;

import pl.robocikd.joboffers.domain.login.dto.UserDto;

import java.util.Optional;

public interface LoginRepository {

    Optional<User> findByUsername(String username);

    User save(User user);
}
