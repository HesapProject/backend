package uz.hesap.service.common.util.message;

import java.time.Instant;
import java.util.Map;
import uz.hesap.service.common.util.enums.Activity;

/**
 * Foydalanuvchi amalini log-servisga (RabbitMQ) yuborish uchun xabar. routingKey = "ActivityLog".
 * Har qanday servis amal bajarilganda publish qiladi: jmsPublisher.publish(new ActivityLog(...)).
 * data — amalga oid ixtiyoriy tafsilotlar JSON (masalan {contractId, number, amount}).
 * actorIn — amalni bajargan foydalanuvchi IN (jismoniy PINFL / yuridik STIR), user_id emas.
 * companyIn — kompaniya STIR (kompaniya konteksti bo'lsa), company_id emas.
 */
public record ActivityLog(
    String actorIn, String companyIn, Activity activity, Map<String, Object> data, Instant time) {}
