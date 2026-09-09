package uz.hesap.service.document.model;

import java.util.List;
import uz.hesap.service.document.model.request.DocumentValueRequest;
import uz.hesap.service.document.model.response.TemplateFieldResponse;
import uz.hesap.service.document.model.response.TemplateResponse;

public record DocumentJson(
    TemplateResponse templateResponse,
    List<TemplateFieldResponse> fields,
    List<DocumentValueRequest> values) {}
