package uz.hesap.service.main.api.v1;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.util.UserPrincipal;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.common.util.enums.UserType;
import uz.hesap.service.main.model.request.ClientProfileUpdateRequest;
import uz.hesap.service.main.model.request.EImzoVerifyRequest;
import uz.hesap.service.main.model.request.LoginRequest;
import uz.hesap.service.main.model.request.MyIdVerifyRequest;
import uz.hesap.service.main.model.request.PhoneConfirmRequest;
import uz.hesap.service.main.model.request.PhoneSendCodeRequest;
import uz.hesap.service.main.model.request.UserRequest;
import uz.hesap.service.main.model.response.AdminUserResponse;
import uz.hesap.service.main.model.response.AgeBucketCount;
import uz.hesap.service.main.model.response.JwtTokenResponse;
import uz.hesap.service.main.model.response.OneIdPassportResponse;
import uz.hesap.service.main.model.response.PhoneConfirmResponse;
import uz.hesap.service.main.service.UserService;

@Log4j2
@RestController
@RequestMapping("/main/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final uz.hesap.service.main.service.SystemSettingService systemSettingService;

  @PostMapping("/phone/send-code")
  public Mono<Void> phoneSendCode(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PhoneSendCodeRequest request) {
    return userService.phoneSendCode(userPrincipal, request);
  }

  @PostMapping("/phone/confirm")
  public Mono<PhoneConfirmResponse> phoneConfirm(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody PhoneConfirmRequest request) {
    return userService.phoneConfirm(userPrincipal, request);
  }

  //  @PostMapping
  //  public Mono<UserResponse> create(
  //      @AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody UserRequest
  // userRequest) {
  //    return userService.createUser(userRequest);
  //  }
  //
  //  @PutMapping
  //  public Mono<UserResponse> update(
  //      @AuthenticationPrincipal UserPrincipal userPrincipal,
  //      @RequestBody UserUpdateRequest userRequest) {
  //    return userService.updateUser(userPrincipal, userPrincipal.user().id(), userRequest);
  //  }
  //
  //  @DeleteMapping("/{id}")
  //  public Mono<Boolean> delete(
  //      @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable("id") UUID id) {
  //    return userService.deleteUser(userPrincipal, id);
  //  }

  @GetMapping
  public Mono<Page<AdminUserResponse>> getAllPage(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam(required = false) String search,
      // Turi bo'yicha filter — Mijozlar sahifasida faqat CLIENT (jismoniy) /
      // COMPANY (yuridik) yuboriladi.
      @RequestParam(required = false) UserType type,
      @RequestParam(required = false) Boolean isVerified,
      @RequestParam(required = false) Integer contractCountFrom,
      @RequestParam(required = false) Integer contractCountTo,
      @RequestParam(required = false) Double balanceFrom,
      @RequestParam(required = false) Double balanceTo,
      @RequestParam(required = false) java.time.Instant lastVisitFrom,
      @RequestParam(required = false) java.time.Instant lastVisitTo,
      // Ro'yxatdan o'tgan sana oralig'i (statistika davri).
      @RequestParam(required = false) java.time.Instant createdFrom,
      @RequestParam(required = false) java.time.Instant createdTo,
      // Statistika: true bo'lsa faqat haqiqiy PINFL'ga ega mijozlar sanaladi.
      @RequestParam(required = false) Boolean hasPinfl,
      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
      @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
      @RequestParam(required = false) Sort sort) {
    Sort defaultSort = (sort != null) ? sort : Sort.by(Sort.Direction.ASC, "id");
    Pageable pageable = PageRequest.of(page, size, defaultSort);
    return userService.getAllPage(
        userPrincipal,
        search,
        type,
        isVerified,
        contractCountFrom,
        contractCountTo,
        balanceFrom,
        balanceTo,
        lastVisitFrom,
        lastVisitTo,
        createdFrom,
        createdTo,
        hasPinfl,
        pageable);
  }

  // Mijozlar yosh taqsimoti — oraliqda (createdFrom/createdTo) ro'yxatdan o'tgan
  // mijozlar bo'yicha (PINFL'dan SQL agregatsiya). Statistika doirasiy chart uchun.
  @GetMapping("/distribution/age")
  public Flux<AgeBucketCount> ageDistribution(
      @RequestParam(required = false) java.time.Instant createdFrom,
      @RequestParam(required = false) java.time.Instant createdTo) {
    return userService.clientAgeDistribution(createdFrom, createdTo);
  }

  // Mijozlar jinsi taqsimoti — oraliqda.
  @GetMapping("/distribution/gender")
  public Flux<AgeBucketCount> genderDistribution(
      @RequestParam(required = false) java.time.Instant createdFrom,
      @RequestParam(required = false) java.time.Instant createdTo) {
    return userService.clientGenderDistribution(createdFrom, createdTo);
  }

  // Kompaniya xodimlari (Xodimlar sahifasi) — aktiv kompaniya id'si bilan.
  // Eski /company mapping o'chgach so'rov /{in}'ga tushib 404 berardi — qayta qo'shildi.
  @GetMapping("/company")
  public Mono<Page<uz.hesap.service.main.model.UserWithRoleDto>> getCompanyUsers(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam UUID companyId,
      @RequestParam(required = false) String search,
      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
      @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
    Pageable pageable = PageRequest.of(page, size);
    return userService.getCompanyUsers(companyId, search, pageable);
  }

  @GetMapping("/me")
  public Mono<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal userPrincipal) {
    // Admin token uchun JwtConverter sintez qilingan UserResponse'ni
    // principal'ga yozadi (session/device yo'q) — uni shu yerda qaytaramiz,
    // DB lookup'ga bormaymiz.
    if (userPrincipal.user() != null
        && (userPrincipal.user().type() == UserType.ADMIN
            || userPrincipal.user().type() == UserType.SUPER_ADMIN)) {
      return Mono.just(userPrincipal.user());
    }
    return userService.getMe(userPrincipal.user().id(), userPrincipal.sessionId());
  }

  // MyID (web OAuth) orqali profilni tasdiqlash — success'da user.verified=true.
  @PostMapping("/verify-myid")
  public Mono<UserResponse> verifyMyId(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody MyIdVerifyRequest request) {
    return userService.verifyByMyId(userPrincipal, request.code(), request.platform());
  }

  public record AbleIdVerifyBody(String attemptId) {}

  // AbleID orqali profilni tasdiqlash — attempt webhook orqali SUCCESS bo'lgan bo'lishi kerak.
  @PostMapping("/verify-ableid")
  public Mono<UserResponse> verifyAbleId(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody AbleIdVerifyBody request) {
    return userService.verifyByAbleId(userPrincipal, request.attemptId());
  }

  // Shaxs tasdiqlash provayderi (MY_ID | ABLE_ID) — mobil ilovalar o'qiydi.
  @GetMapping("/identity-provider")
  public Mono<uz.hesap.service.main.api.v1.SettingsController.IdentityProviderDto>
      identityProvider() {
    return systemSettingService
        .getIdentityProvider()
        .map(uz.hesap.service.main.api.v1.SettingsController.IdentityProviderDto::new);
  }

  // E-IMZO orqali yuridik shaxsni tasdiqlash — success'da COMPANY.verified=true.
  @PostMapping("/verify-eimzo")
  public Mono<UserResponse> verifyEImzo(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody EImzoVerifyRequest request,
      ServerHttpRequest httpRequest) {
    return userService.verifyByEImzo(userPrincipal, request.pkcs7(), clientIp(httpRequest));
  }

  // Haqiqiy mijoz IP — gateway/proxy orqasida X-Forwarded-For birinchi qiymati.
  private static String clientIp(ServerHttpRequest request) {
    String xff = request.getHeaders().getFirst("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      return xff.split(",")[0].trim();
    }
    String realIp = request.getHeaders().getFirst("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
      return realIp.trim();
    }
    var remote = request.getRemoteAddress();
    return remote != null && remote.getAddress() != null
        ? remote.getAddress().getHostAddress()
        : "127.0.0.1";
  }

  // C2C: o'z profilini tahrirlash
  @PutMapping()
  public Mono<UserResponse> updateProfile(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody ClientProfileUpdateRequest request) {
    return userService.updateClientProfile(userPrincipal.user().id(), request);
  }

  // C2C: o'z akkauntini o'chirish (soft delete).
  @DeleteMapping("/me")
  public Mono<Void> deleteMe(@AuthenticationPrincipal UserPrincipal userPrincipal) {
    return userService.deleteMe(userPrincipal.user().id());
  }

  // UUID id bo'yicha to'liq user (vendor mijoz/shartnoma tomon → mijoz info sahifasi).
  // Routing: 3-segmentli (/users/id/{id}) — bir segmentli /{in} bilan to'qnashmaydi.
  @GetMapping("/id/{id}")
  public Mono<UserResponse> getById(@PathVariable UUID id) {
    return userService.findById(id);
  }

  // IN (PINFL/TIN) bo'yicha CLIENT user (jismoniy shaxs) qidirish.
  // Mobile clientlar va vendor frontning "Fuqaroni tekshirish" sahifasi
  // shu endpoint'dan foydalanadi — topilsa UserResponse qaytadi, aks
  // holda 404 NotFoundException.
  @GetMapping("/{in}")
  public Mono<UserResponse> getUserByIn(@PathVariable String in) {
    return userService.getClientByIn(in);
  }

  // Foydalanuvchini id bo'yicha o'chirish (soft delete) — admin amali.
  @DeleteMapping("/{id}")
  public Mono<Boolean> delete(
      @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable UUID id) {
    return userService.deleteUser(userPrincipal, id);
  }
}
