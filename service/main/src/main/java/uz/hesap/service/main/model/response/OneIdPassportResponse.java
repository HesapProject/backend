package uz.hesap.service.main.model.response;

import java.time.LocalDate;
import java.util.UUID;

public record OneIdPassportResponse(
    UUID userId,
    String pin,
    String document,
    String docNum,
    String fullName,
    //    String surnameLatin,
    //    String nameLatin,
    //    String patronymicLatin,
    //    String surnameCyr,
    //    String nameCyr,
    //    String patronymicCyr,
    //    String surnameEn,
    //    String nameEn,
    String issuePlace,
    LocalDate issueDate,
    LocalDate endDate,
    String birthDate,
    String birthPlace,
    String birthCountry,
    String sex,
    String nationality,
    String citizenship,
    String address,
    //    String liveStatus,
    //    String cadastre,
    String photo) {}
