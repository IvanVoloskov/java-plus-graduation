package ewm.user.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import ewm.exception.NotFoundException;
import ewm.user.dto.AdminUserParam;
import ewm.user.dto.UserDto;
import ewm.user.dto.UserPostDto;
import ewm.user.mapper.UserMapper;
import ewm.user.model.QUser;
import ewm.user.model.User;
import ewm.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDto create(UserPostDto userPostDto) {
        User user = userMapper.userPostDtoToUser(userPostDto);
        User savedUser = userRepository.save(user);
        log.info("Created new user {}", savedUser);
        return userMapper.userToUserDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> findAll(AdminUserParam params) {
        Iterable<User> users;
        Pageable pageSelected = PageRequest.of(params.from(), params.size(), Sort.by("id"));

        if (params.ids() == null || params.ids().isEmpty()) {
            log.info("Return all users");
            users = userRepository.findAll(pageSelected);
        } else {
            BooleanExpression byUserIds = QUser.user.id.in(params.ids());

            log.info("Return users with ids={}", params.ids());
            users = userRepository.findAll(byUserIds, pageSelected);
        }

        List<UserDto> usersDto = StreamSupport.stream(users.spliterator(), false)
                .map(userMapper::userToUserDto)
                .toList();

        return usersDto;
    }

    @Override
    public void delete(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () ->  new NotFoundException(String.format("User with id=%d was not found", userId)));
        log.info("Delete user with id {}", userId);
        userRepository.delete(user);
    }

    @Override
    public User findById(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () ->  new NotFoundException(String.format("User with id=%d was not found", userId)));
        return user;
    }
}
