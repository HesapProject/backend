package uz.hesap.service.integration.model.myid;

// document-service uchun MyID natijasining minimal qismi: imzolovchini aniqlash.
// pinfl — yuz tekshiruvidan o'tgan shaxs JSHSHIR'i; comparisonValue — yuz mosligi bali.
public record MyIdVerifyResponse(
    String pinfl,
    Double comparisonValue,
    String firstName,
    String lastName,
    String middleName,
    String passport,
    String issuedBy,
    String issueDate,
    String expiryDate,
    String birthDate,
    String birthPlace,
    String nationality,
    String citizenship,
    String address) {

  // Minimal (WEB / faqat pinfl) — passport detallarsiz.
  public MyIdVerifyResponse(String pinfl, Double comparisonValue) {
    this(pinfl, comparisonValue, null, null, null, null, null, null, null, null, null, null, null, null);
  }
}
