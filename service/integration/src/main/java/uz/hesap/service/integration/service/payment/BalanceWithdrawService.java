package uz.hesap.service.integration.service.payment;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.hesap.service.common.exception.BadRequestException;
import uz.hesap.service.integration.domain.TransactionEntity;
import uz.hesap.service.integration.domain.enums.BalanceType;
import uz.hesap.service.integration.domain.enums.TransactionType;
import uz.hesap.service.integration.repository.BalanceRepository;
import uz.hesap.service.integration.repository.TransactionRepository;

// Balansdan yechish (paket/tarif xaridi). SUMMA balansidan summani ayiradi va
// transaction'ga − qator yozadi (Payme/Click DEPOSIT'ning teskarisi).
@Service
@RequiredArgsConstructor
@Log4j2
public class BalanceWithdrawService {

  private final BalanceHelper balanceHelper;
  private final BalanceRepository balanceRepository;
  private final TransactionRepository transactionRepository;

  public Mono<Void> withdraw(UUID uniqueId, Double amount, TransactionType type, String description) {
    double value = amount != null ? amount : 0;
    if (uniqueId == null || value <= 0) {
      return Mono.error(new BadRequestException("uniqueId va musbat amount majburiy"));
    }
    return balanceHelper
        .getBalanceEntity(uniqueId, BalanceType.SUMMA)
        .flatMap(
            balance -> {
              double current = balance.getBalance() != null ? balance.getBalance() : 0;
              if (current < value) {
                return Mono.error(new BadRequestException("Balansda mablag' yetarli emas"));
              }
              TransactionEntity tx = new TransactionEntity();
              tx.setUserId(uniqueId);
              tx.setBalanceId(balance.getId());
              tx.setBillingType(balance.getBillingType());
              tx.setType(type != null ? type : TransactionType.WITHDRAWAL);
              tx.setAmount(-value); // − li (yechish)
              tx.setDescription(description);
              return transactionRepository
                  .save(tx)
                  .flatMap(
                      saved -> {
                        balance.setBalance(current - value);
                        return balanceRepository.save(balance);
                      })
                  .then();
            });
  }
}
