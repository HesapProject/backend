# Code Review Checklist

Ushbu hujjat loyihadagi har bir yangi kod (Pull Request / Merge Request) yoki AI tomonidan yozilgan kod qismini tekshirish uchun standart qoidalar to'plamidir. Har safar kod yozilganda yoki refaktor qilinganda, ushbu bandlar bo'yicha tekshiruv o'tkazilishi shart.

## 1. Xavfsizlik va Ruxsatlar (Security & Authorization)
- [ ] **Ruxsatlarni tekshirish:** Foydalanuvchi faqat o'ziga tegishli hujjatlar va to'lov ma'lumotlarini ko'rayotganiga/o'zgartirayotganiga ishonch hosil qilinganmi? (Boshqa foydalanuvchilarning ma'lumotlariga ruxsatsiz kirish yopilganmi?)
- [ ] **Ma'lumotlarni tasdiqlash (Validation):** API orqali kelayotgan barcha kiruvchi ma'lumotlar (payload/DTO) qat'iy tekshiruvdan (validation) o'tganmi?
- [ ] **Maxfiy ma'lumotlar:** Kod ichida ochiq parol, API kalitlar yoki tokenlar qolib ketmaganmi? (.env faylidan olinayotganiga ishonch hosil qiling).
- [ ] **SQL Inyeksiyalar himoyasi:** Ma'lumotlar bazasiga so'rov yuborishda xavfsiz metodlardan foydalanilganmi?

## 2. Biznes Mantiq va Arxitektura (Business Logic & Architecture)
- [ ] **Tranzaksiyalar xavfsizligi:** To'lov yoki shartnoma yaratish kabi bir nechta qadamdan iborat jarayonlarda xatolik yuz bersa, barcha o'zgarishlar bekor qilinadimi (Rollback / Database Transactions)?
- [ ] **DRY (Don't Repeat Yourself):** Takrorlanuvchi kodlar bormi? Ularni umumiy yordamchi (utility) funksiyalarga yoki servislarga chiqarish mumkinmi?
- [ ] **Qaradiylik (Single Responsibility):** Har bir funksiya yoki klass faqat bitta vazifani bajaryaptimi? (Masalan, hujjat yaratish funksiyasi ichida to'lov mantiqi aralashib ketmaganmi?)

## 3. Ma'lumotlar Bazasi va Ishlash Tezligi (Database & Performance)
- [ ] **N+1 Muammosi:** Ma'lumotlar bazasidan ro'yxat olinayotganda tsikl ichida so'rovlar yuborilmaganmi? (Eager loading yoki JOIN'lardan to'g'ri foydalanilganmi?)
- [ ] **Indekslash:** Ko'p qidiriladigan maydonlar (masalan, user_id, contract_status) bo'yicha bazaga to'g'ri indekslar qo'yilganmi?
- [ ] **So'rovlar optimalligi:** Keraksiz katta ma'lumotlar bazadan tortib olinmayaptimi? (Faqat kerakli ustunlar tanlab olinganmi?)

## 4. Kod Sifati va O'qilishi (Code Quality)
- [ ] **Nomlanish (Naming Conventions):** O'zgaruvchilar, funksiyalar va klasslar nomlari ularning nima ish qilishini aniq tushuntirib turibdimi? (`data1`, `temp` kabi tushunarsiz nomlar yo'qmi?)
- [ ] **Xatoliklarni ushlash (Error Handling):** `try-catch` bloklari to'g'ri ishlatilganmi? Foydalanuvchiga va loglarga tushunarli xatolik xabarlari (HTTP status kodlari bilan) qaytarilayotganiga ishonch hosil qilinganmi?
- [ ] **Izohlar (Comments):** Murakkab algoritmlar va g'ayrioddiy yechimlar nima uchun aynan shunday yozilganligi qisqa izohlab ketilganmi?

## 5. API Dizayn (API Design)
- [ ] **RESTful standartlari:** Endpoint nomlanishi REST standartlariga mosmi? (Masalan, fe'llar emas, otlar ishlatilganmi: `/create-document` emas, `POST /documents`).
- [ ] **Javob formati (Response Format):** API qaytarayotgan javob (JSON) loyihadagi umumiy standart strukturasiga mos keladimi?