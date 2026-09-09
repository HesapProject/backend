package uz.hesap.service.integration.model.payme;

import java.util.HashMap;

public record ChResult(Boolean allow, HashMap<String, Object> additional, Detail detail) {}
