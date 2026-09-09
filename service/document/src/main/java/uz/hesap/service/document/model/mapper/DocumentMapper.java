package uz.hesap.service.document.model.mapper;

import java.util.List;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import uz.hesap.service.common.util.TemplateBasicResponse;
import uz.hesap.service.common.util.TextModel;
import uz.hesap.service.common.util.UserBasicResponse;
import uz.hesap.service.common.util.UserResponse;
import uz.hesap.service.document.domain.document.DocumentEntity;
import uz.hesap.service.document.domain.document.DocumentValueEntity;
import uz.hesap.service.document.domain.template.TemplateEntity;
import uz.hesap.service.document.model.request.DocumentRequest;
import uz.hesap.service.document.model.response.*;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    uses = InstantMapper.class)
public abstract class DocumentMapper {

  public static final DocumentMapper INSTANCE = Mappers.getMapper(DocumentMapper.class);

  public abstract DocumentEntity toEntity(final DocumentRequest request);

  public abstract void updateEntity(DocumentRequest request, @MappingTarget DocumentEntity entity);

  public abstract DocumentValueResponse toValueResponse(final DocumentValueEntity entity);

  // template entity → common modul dagi TemplateBasicResponse
  public TemplateBasicResponse toTemplateBasic(final TemplateEntity entity) {
    if (entity == null) return null;
    return new TemplateBasicResponse(
        entity.getId(),
        new TextModel(entity.getNameUz(), entity.getNameRu(), entity.getNameEn()),
        new TextModel(entity.getSellerNameUz(), entity.getSellerNameRu(), entity.getSellerNameEn()),
        new TextModel(entity.getBuyerNameUz(), entity.getBuyerNameRu(), entity.getBuyerNameEn()),
        entity.getExchangeMode() != null ? entity.getExchangeMode().name() : "GOODS");
  }

  // enriched response — user, template ma'lumotlari bilan
  @Mapping(source = "entity.id", target = "id")
  @Mapping(source = "entity.templateId", target = "templateId")
  @Mapping(source = "entity.deleted", target = "deleted")
  @Mapping(source = "entity.createdDate", target = "createdDate")
  @Mapping(source = "entity.lastModifiedDate", target = "lastModifiedDate")
  @Mapping(source = "entity.version", target = "version")
  @Mapping(source = "entity.buyerStatus", target = "buyerStatus")
  @Mapping(source = "entity.sellerStatus", target = "sellerStatus")
  @Mapping(source = "entity.buyerIn", target = "buyerIn")
  @Mapping(source = "entity.sellerIn", target = "sellerIn")
  // creatorIn explicit map — multi-source mappingda avtomatik topilmay NULL qolardi
  // (imzolashda paket yechish va ko'rinish qoidasi shu maydonga tayanadi).
  @Mapping(source = "entity.creatorIn", target = "creatorIn")
  @Mapping(source = "buyer", target = "buyer")
  @Mapping(source = "seller", target = "seller")
  @Mapping(source = "createdBy", target = "createdBy")
  @Mapping(source = "template", target = "template")
  @Mapping(source = "values", target = "values")
  @Mapping(source = "products", target = "products")
  public abstract DocumentEnrichedResponse toEnrichedResponse(
      DocumentEntity entity,
      UserBasicResponse buyer,
      UserBasicResponse seller,
      UserBasicResponse createdBy,
      TemplateBasicResponse template,
      List<DocumentValueResponse> values,
      List<ContractProductResponse> products);

  // C2C document response — user, witness, documentContent bilan
  @Mapping(source = "entity.id", target = "id")
  @Mapping(source = "entity.number", target = "number")
  @Mapping(source = "entity.status", target = "status")
  @Mapping(source = "entity.buyerStatus", target = "buyerStatus")
  @Mapping(source = "entity.sellerStatus", target = "sellerStatus")
  @Mapping(source = "entity.price", target = "price")
  @Mapping(source = "entity.currency", target = "currency")
  @Mapping(source = "entity.initialPayment", target = "initialPayment")
  @Mapping(source = "entity.deliveryAt", target = "deliveryAt")
  @Mapping(source = "entity.deleted", target = "deleted")
  @Mapping(source = "entity.createdDate", target = "createdDate")
  @Mapping(source = "entity.lastModifiedDate", target = "lastModifiedDate")
  @Mapping(source = "entity.version", target = "version")
  @Mapping(source = "buyer", target = "buyer")
  @Mapping(source = "seller", target = "seller")
  @Mapping(source = "witnesses", target = "witnesses")
  @Mapping(source = "products", target = "products")
  @Mapping(source = "cancelRequests", target = "cancelRequests")
  @Mapping(source = "documentContent", target = "documentContent")
  public abstract C2CDocumentResponse toC2CResponse(
      DocumentEntity entity,
      UserResponse buyer,
      UserResponse seller,
      List<WitnessResponse> witnesses,
      List<ContractProductResponse> products,
      List<uz.hesap.service.document.model.response.CancelRequestResponse> cancelRequests,
      Object documentContent);
}
