package uz.hesap.service.document.model.response;

// Oldi-berdi statistikasi: kiruvchi/chiquvchi mahsulotlar soni va umumiy summasi.
public record ProductLedgerStats(
    long incomeCount, double incomeAmount, long outcomeCount, double outcomeAmount) {}
