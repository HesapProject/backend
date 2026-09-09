package uz.hesap.service.document.model.response;

/** Strukturadan yasalgan JRXML kompilyatsiya tekshiruvi natijasi (saqlamasdan). */
public record CompileCheckResponse(boolean ok, String message) {}
