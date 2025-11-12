package ru.practicum.front.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.practicum.front.service.Dto;
import ru.practicum.front.service.dto.AccountResponse;

class AccountMapperTest {

    private AccountMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(AccountMapper.class);
    }

    @Test
    void toUserProfileCopiesFields() {
        AccountResponse response = new AccountResponse("alice", "Alice", LocalDate.of(1990, 1, 1));
        Dto.UserProfile profile = mapper.toUserProfile(response);
        assertThat(profile.login()).isEqualTo("alice");
        assertThat(profile.name()).isEqualTo("Alice");
        assertThat(profile.birthdate()).isEqualTo(LocalDate.of(1990, 1, 1));
    }

    @Test
    void toUserShortCopiesSubset() {
        AccountResponse response = new AccountResponse("bob", "Bob", LocalDate.of(1992, 2, 2));
        Dto.UserShort dto = mapper.toUserShort(response);
        assertThat(dto.login()).isEqualTo("bob");
        assertThat(dto.name()).isEqualTo("Bob");
    }
}
