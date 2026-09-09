package uz.hesap.service.integration.model.payme;

public record Message(String uz, String ru, String en) {
  public static final Message WRONG_RESPONSE_DATA =
      new Message(
          "Noto'g'ri buyurtma identifikatori",
          "Неверный идентификатор заказа",
          "Wrong data order id");

  public static final Message DATABASE_ERROR =
      new Message("Ma'lumotlar bazasi xatosi", "Ошибка базы данных", "Database error");

  public static final Message ORDER_NOT_FOUND =
      new Message("Order not found", "Заказ не найден", "Buyurtma topilmadi");

  public static final Message WRONG_AMOUNT =
      new Message("Wrong amount", "Неверная сумма", "Noto'g'ri summa");

  public static final Message UNABLE_TO_COMPLETE_OPERATION =
      new Message(
          "Amalni bajarib bo'lmadi",
          "Невозможно завершить операцию",
          "Unable to complete operation");
  public static final Message ORDER_ALREADY_PAID =
      new Message(
          "Buyurtma uchun allaqchon to'langan", "Заказ уже оплачен", "Order has already been paid");

  public static final Message UNABLE_TO_CANCEL_TRANSACTION =
      new Message(
          "To'lovni bekor qilish mumkin emas",
          "Невозможно отменить транзакцию",
          "Unable to cancel transaction");

  public static final Message WRONG_HEADERS =
      new Message("Noto'g'ri sarlavhalar", "Неверные заголовки", "Wrong headers");

  public static final Message TRANSACTION_NOT_FOUND =
      new Message("Tranzaksiya topilmadi", "Transaction not found", "Транзакция не найдена");
}
