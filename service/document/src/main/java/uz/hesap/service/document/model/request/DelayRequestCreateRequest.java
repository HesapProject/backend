package uz.hesap.service.document.model.request;

import java.time.Instant;
import java.util.UUID;

// Kechiktirish so'rovi yaratish: qaysi to'lov jadvali (paymentScheduleId) va yangi sana.
public record DelayRequestCreateRequest(UUID paymentScheduleId, Instant date, String note) {}
