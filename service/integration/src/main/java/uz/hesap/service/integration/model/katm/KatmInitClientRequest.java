package uz.hesap.service.integration.model.katm;

// POST {base_url}/auth/init-client — mijozni KATM'da ro'yxatdan o'tkazib,
// pClientId (KATM-SIR) oladi. Maydon nomlari KATM TZ'siga mos (p* prefiksi).
public record KatmInitClientRequest(
    String pPinfl,
    String pDocSeries,
    String pDocNumber,
    String pFirstName,
    String pLastName,
    String pMiddleName,
    String pBirthDate, // yyyy-mm-dd
    String pIssueDocDate, // yyyy-mm-dd
    String pExpiredDocDate, // yyyy-mm-dd
    Integer pGender, // 1-erkak / 2-ayol
    String pDistrictId,
    String pResAddress,
    String pRegAddress,
    String pPhone) {}
