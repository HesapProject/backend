package uz.hesap.service.integration.model.oneid;

import java.util.UUID;

// main-service'dan kelgan profil saqlash so'rovi.
public record OneIdProfileSaveRequest(UUID userId, OneIdUserResponse data) {}
