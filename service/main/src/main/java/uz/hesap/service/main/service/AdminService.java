package uz.hesap.service.main.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.AlreadyExistsException;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.common.exception.NotFoundException;
import uz.hesap.service.common.util.enums.Role;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.domain.UserEntity;
import uz.hesap.service.main.model.request.AdminCreateRequest;
import uz.hesap.service.main.model.response.AdminResponse;
import uz.hesap.service.main.repository.UserRepository;

// Control admin akkauntlari (type=ADMIN, login+parol) — CRUD. Admin control
// panelga login/parol bilan kiradi (AuthService.login).
@Service
@RequiredArgsConstructor
@Log4j2
public class AdminService {

  private final UserRepository userRepository;

  public Flux<AdminResponse> list() {
    return userRepository.findAdmins().map(this::toResponse);
  }

  public Mono<AdminResponse> create(final AdminCreateRequest req) {
    if (req == null
        || req.login() == null
        || req.login().isBlank()
        || req.password() == null
        || req.password().isBlank()
        || req.firstName() == null
        || req.firstName().isBlank()) {
      return Mono.error(new BadRequestException("Ism, login va parol majburiy"));
    }
    final String login = req.login().trim();
    return userRepository
        .findByLoginAndDeletedFalse(login)
        .flatMap(existing -> Mono.<UserEntity>error(new AlreadyExistsException("Bu login band")))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  UserEntity u = new UserEntity();
                  u.setId(UUID.randomUUID());
                  u.setFirstName(req.firstName().trim());
                  u.setLastName(req.lastName() == null ? null : req.lastName().trim());
                  u.setLogin(login);
                  // Parol BCrypt bilan hash'lanadi — AuthService.login BCrypt.checkpw qiladi.
                  u.setPassword(BCrypt.hashpw(req.password(), BCrypt.gensalt()));
                  u.setType(UserType.ADMIN);
                  u.setRole(Role.ADMIN);
                  return userRepository.save(u);
                }))
        .map(this::toResponse);
  }

  public Mono<Void> delete(final UUID id) {
    return userRepository
        .findByIdAndDeletedIsFalse(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Admin topilmadi")))
        .flatMap(
            u -> {
              u.setDeleted(true);
              return userRepository.save(u);
            })
        .then();
  }

  private AdminResponse toResponse(UserEntity u) {
    return new AdminResponse(
        u.getId(),
        u.getFirstName(),
        u.getLastName(),
        u.getLogin(),
        u.getType(),
        u.getCreatedDate());
  }
}
