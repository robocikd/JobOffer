package pl.robocikd.joboffers.domain.login;

import lombok.RequiredArgsConstructor;
import pl.robocikd.joboffers.domain.login.dto.RegisterUserDto;
import pl.robocikd.joboffers.domain.login.dto.RegistrationResultDto;
import pl.robocikd.joboffers.domain.login.dto.UserDto;

@RequiredArgsConstructor
public class LoginAndRegisterFacade {

    private static final String USER_NOT_FOUND = "User not found";
    private final LoginRepository repository;

    public UserDto findByUsername(String userName) {
        return repository.findByUsername(userName)
                .map(user -> new UserDto(user.id(), user.password(), user.username()))
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
    }

    public RegistrationResultDto register(RegisterUserDto registerUserDto) {
        final User user = User.builder()
                .username(registerUserDto.username())
                .password(registerUserDto.password())
                .build();
        User savedUser = repository.save(user);
        return new RegistrationResultDto(savedUser.id(), true, savedUser.username());
    }
}
