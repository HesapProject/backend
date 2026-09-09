package uz.hesap.service.integration.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.integration.domain.PaymeTransactionEntity;
import uz.hesap.service.integration.model.payme.Account;
import uz.hesap.service.integration.model.payme.PaymeTransactionModel;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = InstantMapper.class)
public abstract class PaymeMapper {
  public static final PaymeMapper INSTANCE = Mappers.getMapper(PaymeMapper.class);

  public PaymeTransactionModel convertEntityToMap(PaymeTransactionEntity paymeTransactionEntity) {
    return new PaymeTransactionModel(
        paymeTransactionEntity.getPaycomId(),
        paymeTransactionEntity.getPaycomTime(),
        paymeTransactionEntity.getAmount(),
        new Account(paymeTransactionEntity.getUuid()),
        paymeTransactionEntity.getCreateTime(),
        paymeTransactionEntity.getPerformTime(),
        paymeTransactionEntity.getCancelTime(),
        paymeTransactionEntity.getId().toString(),
        paymeTransactionEntity.getState(),
        paymeTransactionEntity.getReason());
  }
}
