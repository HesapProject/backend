package uz.hesap.service.document.domain.template;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import uz.hesap.service.document.domain.enums.ExchangeMode;
import uz.hesap.service.document.domain.enums.RoumingType;
import uz.hesap.service.document.domain.enums.TemplateStatus;
import uz.hesap.service.document.domain.enums.TemplateType;
import uz.hesap.service.document.domain.enums.VerificationType;
import uz.hesap.service.document.util.Constants;

@RequiredArgsConstructor
@Getter
@Setter
@Table(schema = Constants.SCHEMA, name = Constants.TABLE_TEMPLATE)
public class TemplateEntity {
  @Id private UUID id;
  private UUID templateId;
  private UUID companyId;
  private String nameUz;
  private String nameRu;
  private String nameEn;
  // Shartnoma taraflari nomi (3 til). first = birinchi taraf (Oluvchi/Qarzdor),
  // second = ikkinchi taraf (Sotuvchi/Haqdor).
  private String sellerNameUz;
  private String sellerNameRu;
  private String sellerNameEn;
  private String buyerNameUz;
  private String buyerNameRu;
  private String buyerNameEn;
  // Mahsulot (tovar/xizmat) nomi/yorlig'i — PDF mahsulot ustuni sarlavhasi (taraf nomlaridek).
  private String productNameUz;
  private String productNameRu;
  private String productNameEn;
  // JRXML manbasi 3 tilda (uz/ru/en) — hujjat generatsiyasida foydalanuvchi tiliga qarab
  // tanlanadi. Backward-compat: eski `template_data` ustuni `template_data_uz`ga rename qilingan.
  private String templateDataUz;
  private String templateDataRu;
  private String templateDataEn;
  // Blok-konstruktor manbasi (blok struktura JSON, TEXT). templateData shu strukturadan
  // generatsiya qilinadi. NULL → struktura yo'q (eski raw-JRXML shablon).
  private String templateStructure;
  private TemplateStatus status;
  private TemplateType templateType;
  // Tasdiqlash turi: jismoniy va yuridik shaxslar uchun alohida.
  private VerificationType individualVerificationType;
  private VerificationType legalVerificationType;
  private Double amount;
  private Integer witnessCount;
  // Yo'l qo'yilgan valyutalar kodi (UZS, USD, ...). NULL → cheklov yo'q.
  private String[] enabledCurrencies;
  // Shablonda mahsulotlar va to'lov jadvali yoqilganmi.
  private Boolean productEnabled = Boolean.TRUE;
  // Mahsulotlar yoqilgan bo'lsa — shartnoma yaratishda majburiymi (kamida 1 ta).
  private Boolean productRequired = Boolean.FALSE;
  private Boolean paymentScheduleEnabled = Boolean.TRUE;
  // Shablonda guvohlik (witness) yoqilganmi (Bor/Yo'q toggle).
  private Boolean witnessEnabled = Boolean.TRUE;
  // Shablonda boshlang'ich to'lov yoqilganmi (Bor/Yo'q toggle). Summa shartnomada qoladi.
  private Boolean initialPaymentEnabled = Boolean.TRUE;
  // Oldi-berdi turi: GOODS (mahsulot, soni) yoki MONEY (qarz, summa). Default GOODS.
  private ExchangeMode exchangeMode = ExchangeMode.GOODS;
  // Rouming (ЭСФ provider) hujjat turi. NULL -> Rouming'ga yuborilmaydi.
  private RoumingType roumingType;
  // Ro'yxatda/shartnoma yaratishda ko'rsatish tartibi — katta qiymat oldinroq chiqadi
  // (ORDER BY priority DESC). Default 0 — priority berilmagan shablonlar oxirida qoladi.
  private Integer priority = 0;
  // Per-event SMS/Firebase notification sozlamalari endi `integration.template_notification`
  // jadvalida (hodisaga ko'ra alohida konfig). Eski umumiy fallback ustunlar olib tashlandi.
  private Boolean deleted = Boolean.FALSE;
  @CreatedDate private Instant createdDate;
  @LastModifiedDate private Instant lastModifiedDate;
}
