package uz.hesap.service.document.model.response.eimzo;

import java.util.List;

public record SignerInfo(
    SignerId signerId,
    String signingTime,
    String signature,
    String digest,
    TimeStampInfo timeStampInfo,
    List<CertificateInfo> certificate,
    String OCSPResponse,
    String statusUpdatedAt,
    String statusNextUpdateAt,
    Boolean verified,
    Boolean certificateVerified,
    CertificateInfo trustedCertificate,
    List<String> policyIdentifiers,
    Boolean certificateValidAtSigningTime) {}
