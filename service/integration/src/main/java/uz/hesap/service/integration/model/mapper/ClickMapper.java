package uz.hesap.service.integration.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.integration.model.click.ClickCompleteResponse;
import uz.hesap.service.integration.model.click.ClickFiscalizationModel;
import uz.hesap.service.integration.model.click.ClickPrepareResponse;
import uz.hesap.service.integration.model.click.ClickResponse;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = InstantMapper.class)
public abstract class ClickMapper {
  public static final ClickMapper INSTANCE = Mappers.getMapper(ClickMapper.class);

  public ClickPrepareResponse toClickPrepare(final ClickResponse click) {
    ClickPrepareResponse clickResp = new ClickPrepareResponse();
    clickResp.setError(click.getError());
    clickResp.setError_note(click.getError_note());
    clickResp.setClick_trans_id(click.getClick_trans_id());
    clickResp.setMerchant_trans_id(click.getMerchant_trans_id());
    clickResp.setMerchant_prepare_id(click.getMerchant_prepare_id());
    return clickResp;
  }

  public ClickCompleteResponse toClickComplete(final ClickResponse response) {
    ClickCompleteResponse clickResp = new ClickCompleteResponse();
    clickResp.setError(response.getError());
    clickResp.setErrorNote(response.getError_note());
    clickResp.setClickTransId(response.getClick_trans_id());
    clickResp.setMerchantTransId(response.getMerchant_trans_id());
    clickResp.setMerchantConfirmId(response.getMerchant_confirm_id());
    if (response.getFiscalization() != null) {
      final ClickFiscalizationModel fiscalization = response.getFiscalization();
      clickResp.setPaymentId(fiscalization.paymentId());
      clickResp.setServiceId(fiscalization.serviceId());
      clickResp.setReceivedCard(fiscalization.receivedCard());
      clickResp.setReceivedCash(fiscalization.receivedCash());
      clickResp.setReceivedEcash(fiscalization.receivedEcash());
      clickResp.setItems(fiscalization.items());
    }
    return clickResp;
  }
}
