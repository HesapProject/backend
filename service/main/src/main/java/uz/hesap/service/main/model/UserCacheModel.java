package uz.hesap.service.main.model;

import java.io.Serializable;

public record UserCacheModel(String firstName, String lastName, String phone, String code)
    implements Serializable {}
