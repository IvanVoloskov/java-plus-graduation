package ewm.user.service;

import ewm.user.dto.AdminUserParam;
import ewm.user.dto.UserDto;
import ewm.user.dto.UserPostDto;
import ewm.user.model.User;

import java.util.List;

public interface UserService {
    UserDto create(UserPostDto userPostDto);

    List<UserDto> findAll(AdminUserParam params);

    void delete(Long userId);

    User findById(Long userId);
}
