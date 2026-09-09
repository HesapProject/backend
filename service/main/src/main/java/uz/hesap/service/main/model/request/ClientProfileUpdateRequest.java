package uz.hesap.service.main.model.request;

// C2C client o'z profilini tahrirlash uchun
public record ClientProfileUpdateRequest(
    String image,
    //    String phone,
    String secondPhone) {}
