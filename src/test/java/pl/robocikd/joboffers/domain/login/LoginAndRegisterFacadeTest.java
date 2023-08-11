package pl.robocikd.joboffers.domain.login;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import pl.robocikd.joboffers.domain.login.dto.RegisterUserDto;
import pl.robocikd.joboffers.domain.login.dto.RegistrationResultDto;
import pl.robocikd.joboffers.domain.login.dto.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;

class LoginAndRegisterFacadeTest {

    InMemoryLoginRepository repository = new InMemoryLoginRepository();
    LoginAndRegisterFacade loginFacade = new LoginAndRegisterFacade(repository);

    @Test
    void should_register_user() {
        // given
        RegisterUserDto registerUserDto = new RegisterUserDto("username", "pass");
        // when
        RegistrationResultDto register = loginFacade.register(registerUserDto);
        // then
        assertAll(
                () -> assertThat(register.created()).isTrue(),
                () -> assertThat(register.username()).isEqualTo("username")
        );
    }

    @Test
    void should_find_user_by_user_name() {
        // given
        RegisterUserDto registerUserDto = new RegisterUserDto("name", "pass");
        RegistrationResultDto register = loginFacade.register(registerUserDto);
        // when
        UserDto userDto = loginFacade.findByUsername("name");
        // then
        assertThat(userDto).isEqualTo(new UserDto(register.id(), "pass", "name"));
    }

    @Test
    void should_throw_exception_when_user_not_found() {
        // given
        String name = "SomeUser";
        // when
        Throwable thrown = catchThrowable(() -> loginFacade.findByUsername(name));
        // then
        AssertionsForClassTypes.assertThat(thrown)
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }

}