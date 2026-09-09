package uz.hesap.service.main.service;

import static uz.hesap.service.common.exception.handler.ErrorCode.*;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.AlreadyExistsException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.common.util.enums.Role;
import uz.hesap.service.main.domain.*;
import uz.hesap.service.main.model.mapper.EmployeeMapper;
import uz.hesap.service.main.model.mapper.UserMapper;
import uz.hesap.service.main.model.request.EmployeeRequest;
import uz.hesap.service.main.model.request.EmployeeUpdateRequest;
import uz.hesap.service.main.model.response.EmployeeResponse;
import uz.hesap.service.main.repository.UserRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class EmployeeService {

  private final UserRepository userRepository;

  public Mono<EmployeeResponse> createUser(EmployeeRequest request) {
    return userRepository
        .existsByPhoneAndDeletedFalseAndType(
            request.phone(), uz.hesap.service.common.util.enums.UserType.ADMIN)
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(
                    new AlreadyExistsException(ALREADY_EXISTS_ERROR_CODE, "Phone already exists"));
              }
              UserEntity userEntity = EmployeeMapper.INSTANCE.toEntity(request);
              userEntity.setType(uz.hesap.service.common.util.enums.UserType.ADMIN);
              return userRepository.save(userEntity);
            })
        .map(EmployeeMapper.INSTANCE::toResponse);
  }

  public Mono<EmployeeResponse> updateUser(UUID id, EmployeeUpdateRequest request) {
    return userRepository
        .findByIdAndDeletedIsFalse(id)
        .switchIfEmpty(Mono.error(new NotFoundException(USER_NOT_FOUND, "User not found")))
        .flatMap(
            userEntity ->
                userRepository
                    .existsByPhoneAndTypeAndDeletedFalseAndIdNot(
                        request.phone(), uz.hesap.service.common.util.enums.UserType.ADMIN, id)
                    .flatMap(
                        exists -> {
                          if (exists) {
                            return Mono.error(
                                new AlreadyExistsException(
                                    ALREADY_EXISTS_ERROR_CODE, "Phone already exists"));
                          }
                          userEntity.setFirstName(request.firstName());
                          userEntity.setLastName(request.lastName());
                          userEntity.setPhone(request.phone());
                          userEntity.setRole(request.role());
                          return userRepository.save(userEntity);
                        }))
        .map(EmployeeMapper.INSTANCE::toResponse);
  }

  public Mono<Page<EmployeeResponse>> getEmployees(String search, Role role, Pageable pageable) {
    return userRepository
        .findEmployees(search, role, pageable)
        .collectList()
        .zipWith(userRepository.countEmployees(search, role))
        .map(
            tuple ->
                new PageImpl<>(
                    tuple.getT1().stream().map(EmployeeMapper.INSTANCE::toResponse).toList(),
                    pageable,
                    tuple.getT2()));
  }
}
