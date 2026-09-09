package uz.hesap.service.main.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TemplateApplicationResponse(UUID id, Double price) {}
