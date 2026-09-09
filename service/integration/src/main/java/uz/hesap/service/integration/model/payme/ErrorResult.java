package uz.hesap.service.integration.model.payme;

import uz.hesap.service.integration.util.PaymeInterface;

public record ErrorResult(Error error, Long id) implements PaymeInterface {}
