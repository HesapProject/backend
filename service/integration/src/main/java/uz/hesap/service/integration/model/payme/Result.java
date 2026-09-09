package uz.hesap.service.integration.model.payme;

import uz.hesap.service.integration.util.PaymeInterface;

public record Result(Object result) implements PaymeInterface {}
