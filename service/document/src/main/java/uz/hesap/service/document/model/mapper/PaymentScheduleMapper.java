package uz.hesap.service.document.model.mapper;

import java.util.UUID;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.common.util.UserBasicResponse;
import uz.hesap.service.document.domain.payment.PaidScheduleEntity;
import uz.hesap.service.document.domain.payment.PaymentEntity;
import uz.hesap.service.document.domain.payment.PaymentScheduleRequestEntity;
import uz.hesap.service.document.model.request.PaidScheduleRequest;
import uz.hesap.service.document.model.request.PaymentScheduleRequest;
import uz.hesap.service.document.model.request.PaymentScheduleRequestRequest;
import uz.hesap.service.document.model.response.*;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    uses = InstantMapper.class)
public abstract class PaymentScheduleMapper {

  public static final PaymentScheduleMapper INSTANCE =
      Mappers.getMapper(PaymentScheduleMapper.class);

  public abstract PaymentEntity toEntity(final PaymentScheduleRequest request);

  @Mapping(target = "buyer", source = "buyer")
  @Mapping(target = "seller", source = "seller")
  @Mapping(target = "id", source = "entity.id")
  public abstract PaymentScheduleResponse toResponsePayment(
      final PaymentEntity entity, UserBasicResponse buyer, UserBasicResponse seller);

  public abstract void updateEntity(
      PaymentScheduleRequest request, @MappingTarget PaymentEntity entity);

  @Mapping(target = "contractId", source = "documentId")
  public abstract PaymentScheduleRequestEntity toEntity(
      final PaymentScheduleRequestRequest request, final UUID documentId);

  public abstract PaymentScheduleRequestResponse toResponseRequest(
      final PaymentScheduleRequestEntity entity);

  public abstract void updateEntity(
      PaymentScheduleRequestRequest request, @MappingTarget PaymentScheduleRequestEntity entity);

  public abstract void updateEntity(
      PaymentScheduleRequestEntity source, @MappingTarget PaymentEntity target);

  public abstract PaidScheduleEntity toEntity(final PaidScheduleRequest request);

  // PaidSchedule — YANGI yozuv: payment'ning id/audit'ini ko'chirmaymiz, aks holda
  // R2DBC INSERT o'rniga UPDATE qiladi → "row with id [...] does not exist".
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "lastModifiedDate", ignore = true)
  public abstract void toPaid(
      @MappingTarget final PaidScheduleEntity target, final PaymentEntity request);

  @Mapping(target = "buyer", source = "buyer")
  @Mapping(target = "seller", source = "seller")
  @Mapping(target = "id", source = "entity.id")
  public abstract PaidScheduleResponse toResponsePayment(
      final PaidScheduleEntity entity, UserBasicResponse buyer, UserBasicResponse seller);

  public abstract void updateEntity(
      PaidScheduleRequest request, @MappingTarget PaidScheduleEntity entity);

  public abstract void toDelay(
      @MappingTarget PaymentScheduleRequestEntity entity, PaymentEntity p);

  // ================ enriched responses ================

  @Mapping(target = "buyer", source = "buyer")
  @Mapping(target = "seller", source = "seller")
  @Mapping(target = "id", source = "entity.id")
  @Mapping(target = "documentNumber", source = "documentNumber")
  public abstract B2BPaymentScheduleResponse toB2BPaymentResponse(
      PaymentEntity entity,
      String documentNumber,
      UserBasicResponse buyer,
      UserBasicResponse seller);

  @Mapping(target = "buyer", source = "buyer")
  @Mapping(target = "seller", source = "seller")
  @Mapping(target = "id", source = "entity.id")
  @Mapping(target = "documentNumber", source = "documentNumber")
  public abstract B2BPaidScheduleResponse toB2BPaidResponse(
      PaidScheduleEntity entity,
      String documentNumber,
      UserBasicResponse buyer,
      UserBasicResponse seller);

  protected double calcUnpaid(PaymentEntity entity) {
    double amount = entity.getTotalAmount() != null ? entity.getTotalAmount() : 0.0;
    double paid = entity.getPaidAmount() != null ? entity.getPaidAmount() : 0.0;
    return amount - paid;
  }
}
