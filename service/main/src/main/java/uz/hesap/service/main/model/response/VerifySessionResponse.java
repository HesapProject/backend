package uz.hesap.service.main.model.response;

import java.io.Serializable;
import java.util.UUID;

public record VerifySessionResponse(
    UUID id,
    String phone,
    String uuid,
    String osVersion,
    String os,
    String model,
    String brand,
    String type,
    String device,
    String fcmToken)
    implements Serializable {}
