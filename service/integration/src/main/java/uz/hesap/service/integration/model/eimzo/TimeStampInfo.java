package uz.hesap.service.integration.model.eimzo;

import java.util.List;

public record TimeStampInfo(
    List<CertificateInfo> certificate,
    String OCSPResponse,
    String statusUpdatedAt,
    String statusNextUpdateAt,
    Boolean digestVerified,
    Boolean certificateVerified,
    CertificateInfo trustedCertificate,
    Boolean certificateValidAtSigningTime,
    SignerId signerId,
    String tsaPolicy,
    String time,
    String hashAlgorithm,
    String serialNumber,
    String tsa,
    String messageImprintAlgOID,
    String messageImprintDigest,
    Boolean verified) {}
