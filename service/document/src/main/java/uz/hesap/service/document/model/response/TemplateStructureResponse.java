package uz.hesap.service.document.model.response;

import java.util.List;

/**
 * Blok-konstruktor saqlash natijasi: saqlangan struktura (JSON), undan generatsiya qilingan
 * JRXML, va sinxronlangan template_field'lar.
 */
public record TemplateStructureResponse(
    String structure, String jrxml, List<TemplateFieldResponse> fields) {}
