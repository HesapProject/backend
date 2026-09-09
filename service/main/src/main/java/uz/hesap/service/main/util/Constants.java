package uz.hesap.service.main.util;

public record Constants() {

  public static final String SCHEMA = "\"user\"";
  public static final String TABLE_USER = "\"user\"";
  public static final String TABLE_SESSION = "session";
  public static final String TABLE_PERMISSION_REQUEST = "white_list_request";
  public static final String TABLE_USER_PERMISSION = "white_list";
  public static final String TABLE_SOCIAL = "socials";
  public static final String TABLE_SYSTEM_SETTING = "system_setting";
  // CMS (notification servisidan ko'chirildi)
  public static final String TABLE_NEWS = "news";
  public static final String TABLE_BANNER = "banner";
  public static final String TABLE_STORY = "story";
  public static final String TABLE_STORY_VIEW = "story_view";
  public static final String TABLE_FAQ = "faq";
  public static final String TABLE_DOCS = "docs";
  // Billing (billing servisidan ko'chirildi)
  public static final String TABLE_PACKAGES = "packages";
  public static final String TABLE_PROMOS = "promos";
  public static final String TABLE_USER_PACKAGE_USAGE = "user_package_usage";
  // Whitelist (from→to ruxsat berish: hujjat/skoring/hamkor/shartnoma)
  // To'lovlar jurnali (Payme/Click/Control)
  public static final String TABLE_PAYMENTS = "payments";
  // Xaridlar jurnali (tarif/paket sotib olish)
  public static final String TABLE_PURCHASES = "purchases";
  // Foydalanuvchi paketi + har shablon limit/usage
  public static final String TABLE_USER_PACKAGE = "user_package";
  // OpenAPI kalitlari (tashqi integratsiya)
  public static final String TABLE_API_KEY = "api_key";
}
