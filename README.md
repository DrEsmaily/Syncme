<p align="center"><img src="https://raw.githubusercontent.com/DrEsmaily/element-x-android/syncme-cumulative-build-20261015/appicon/element/src/main/res/drawable-nodpi/syncme_launcher.webp" alt="SyncMe app icon" width="144" /></p>

# SyncMe for Android

**SyncMe** یک پیام‌رسان اندرویدی مبتنی بر Matrix و انشعابی مستقل از [Element X Android](https://github.com/element-hq/element-x-android) است.
رابط اختصاصی، برچسب‌های شخصی گفتگو، حضور مشترک کاربران و ابزارهای تشخیص اختیاری، در این مخزن توسعه می‌یابند.

SyncMe is an independent fork of Element X Android, built with Matrix Rust SDK, Jetpack Compose and Appyx. This repository contains SyncMe-specific development; it is not an official Element distribution.

## آخرین نسخه و دانلود

- **آخرین APK تأییدشده: SyncMe 253، ARM64** — [اجرا و دانلود فایل](https://github.com/DrEsmaily/element-x-android/actions/runs/38658866967)، کد `d101f1244a03c82f545dd9da0de1c477ae887ba7`.
- **SyncMe 254**: بازسازی همان کد با مستندات تجمیعی؛ [وضعیت ساخت و دانلود](https://github.com/DrEsmaily/element-x-android/actions/workflows/syncme-cumulative.yml). تا سبزشدن اجرای جدید، نسخهٔ 253 آخرین بیلد تأییدشده است.
- [گزارش تجمیعی فارسی از شروع توسعه تا نسخهٔ فعلی](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/cumulative-release-notes-fa.md)
- [جزئیات تمام commitهای نسخهٔ فعلی و مسیر فایل‌های تغییرکرده](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/history/current-lineage.md)
- [ده بیلد موفق محفوظ و شناسهٔ فایل‌هایشان](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/history/protected-builds.json)

دانلود artifact از Actions ممکن است به ورود به GitHub نیاز داشته باشد. فایل‌های Actions تاریخ انقضا دارند؛ این انتشار یک نسخهٔ پشتیبان دیگر از ده بیلد قبلی با نگهداری ۹۰روزه تهیه می‌کند. این نسخهٔ پشتیبان، میزبانی دائمی دانلود نیست.

## قابلیت‌های اضافه‌شده و سفارشی‌شده در SyncMe

این‌ها تغییرات SyncMe نسبت به نسخهٔ پایهٔ Element X هستند؛ جزئیات پیاده‌سازی و محدودیت هر مورد در گزارش تجمیعی آمده است.

| قابلیت | کجا و چه کاری انجام می‌دهد؟ |
|---|---|
| **نمایش آنلاین و Away** | نشانگر مشترک حضور در خانه، سربرگ گفتگو، پروفایل و فهرست اعضا؛ Online سبز و Away نارنجی، بر اساس پاسخ واقعی Matrix. |
| **آخرین مشاهده و تعداد آنلاین** | متن انگلیسی آخرین مشاهده و شمارش اعضای آنلاین گروه؛ در دادهٔ ناقص با قید «حداقل»، بدون حدس‌زدن وضعیت ناشناخته. |
| **درصد و حجم آپلود** | نمایش درصد واقعی، حجم منتقل‌شده/کل و نوار پیشرفت داخل گفتگو، با دادهٔ صف ارسال SDK و چیدمان متناسب حباب. |
| **کنترل‌های ارسال رسانه** | تغییرات انتخاب کیفیت و نگهداری اصل فایل، لغو آپلود از منوی پیام و اصلاح‌های نمایش وضعیت ارسال. |
| **برچسب‌های شخصی گفتگو** | ساخت، تغییر نام و مدیریت مستقل، انتساب گفتگو و فیلترهای پایدار خانه؛ حداکثر سه برچسب شخصی. |
| **طراحی و هویت SyncMe** | نام و نشان SM، خوشامدگویی اختصاصی، رنگ‌های کبالت و زمینه‌های مشکی، تغییرات منوها، واکنش‌ها و تنظیمات. |
| **گزارش‌های تشخیصی اختیاری** | ابزارهای startup، TLS و رمزگشایی در Developer Options؛ پیش‌فرض خاموش و با ثبت محدود. |
| **رفتار تکمیلی رسید خواندن** | آواتار خواننده زیر پیام خروجی مرتبط، retry رسید ناموفق و محدودکردن رسید خودکار به گفتگوی فعال و پیام مشاهده‌شده. |
| **بستن گروه با مجوز مدیر** | اقدام تأییدشدهٔ سمت کلاینت؛ تضمین حذف تمام نسخه‌های پیام از دستگاه‌ها و سرورهای فدره‌شده نیست. |

### امکانات به‌ارث‌رسیده از Element X و Matrix

ورود و حساب Matrix، گفتگوی مستقیم و گروهی، زیرساخت رمزنگاری سرتاسری، پیام و رسانه، همگام‌سازی و پروتکل رسید خواندن از پایهٔ اصلی آمده‌اند. تغییرات بالا لایه‌های اضافه‌شده یا سفارشی‌شدهٔ SyncMe هستند، نه ادعای ساخت این زیرساخت از صفر.

### اصلاحات فنی و محدودیت‌های فعلی

SyncMe اصلاحاتی در پایان بازنشانی هویت، مدیریت توکن FCM، چرخهٔ حضور/رسید و چیدمان پیام دارد. TLS فعلی اعتماد سیستم/انقضا/EKU و hostname را بررسی می‌کند، ولی مرحلهٔ آنلاین ابطال OCSP/CRL حذف شده است؛ [جزئیات فنی و آزمون‌ها](https://github.com/DrEsmaily/element-x-android/blob/syncme-cumulative-build-20261015/docs/syncme/root-cause-review.md).

موفقیت CI با تأیید روی گوشی برابر نیست. بارگذاری فایل بزرگ تحت VPN، حضور چند دستگاه، اعلان پس‌زمینه و رسید دو گوشی به آزمون واقعی نیاز دارند. آزمایش‌های شاخه‌های قدیمی خودبه‌خود قابلیت نسخهٔ فعلی نیستند.

## تصاویر

نشان بالا فایل واقعی لانچر SyncMe است. تصویر زیر، عکس واقعی صفحهٔ ورود SyncMe است که صاحب پروژه ارائه کرده؛ شمارهٔ بیلد در تصویر دیده نمی‌شود.

<p align="center"><img src="https://raw.githubusercontent.com/DrEsmaily/element-x-android/develop/docs/syncme/images/welcome.jpg" alt="صفحهٔ ورود واقعی SyncMe با نشان SM و دکمهٔ Login" width="288" /></p>

## ادامهٔ توسعه و تاریخچه

کد آخرین بیلد موفق در شاخهٔ `fix/settings-display-cleanup` و بازسازی مستندشده در `syncme-cumulative-build-20261015` قرار دارد.
برای ادامه ابتدا گزارش تجمیعی، گزارش حضور و گزارش ریشه‌یابی را بخوانید. تمام ۴۲۲ اجرای موجود در snapshot اولیه، ۷۹۷ commit اختصاصی شاخه‌های بیلد و نسخه‌های یادداشت‌های تاریخی در پوشهٔ `docs/syncme/history` شاخهٔ بازسازی فهرست شده‌اند.

نسخه‌های قدیمی توضیحات ممکن است رفتار منسوخ یا کار ناتمام را توصیف کنند؛ تاریخ، commit و گزارش جدیدتر ملاک هستند.
پاک‌سازی Actions تاریخچهٔ Git را حذف نمی‌کند. ده اجرای موفق قبلی و بیلد مستندشدهٔ جدید باید حفظ شوند.

## ساخت و مشارکت

Android 7.0+ (API 24)، JDK 21 و ابزار Android لازم‌اند. راهنمای [مشارکت](CONTRIBUTING.md) و [راه‌اندازی توسعه](docs/_developer_onboarding.md) را ببینید.
workflow اختصاصی SyncMe آزمون‌های regression را اجرا کرده و `:app:assembleGplayRelease` را با شناسهٔ بستهٔ موجود `.syncmerelease` می‌سازد؛ ARM64، شمارهٔ نسخه، manifest و کلاس JNI بررسی می‌شوند.
گزارش خطای SyncMe را در [همین مخزن](https://github.com/DrEsmaily/element-x-android/issues) ثبت کنید.

## منشأ و حقوق سازندگان

پایهٔ این پروژه [element-hq/element-x-android](https://github.com/element-hq/element-x-android) است؛ تاریخچه و انتساب سازندگان حفظ شده‌اند. فایل‌های [LICENSE](LICENSE) و [LICENSE-COMMERCIAL](LICENSE-COMMERCIAL) مرجع شرایط مجوزند. استفاده از نام SyncMe این شرایط را تغییر نمی‌دهد.

## Copyright and License

Copyright (c) 2025 Element Creations Ltd.
Copyright (c) 2022 - 2025 New Vector Ltd.

This software is dual licensed by Element Creations Ltd (Element). It can be used either:

(1) for free under the terms of the GNU Affero General Public License (as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version); OR

(2) under the terms of a paid-for Element Commercial License agreement between you and Element (the terms of which may vary depending on what you and Element have agreed to).

Unless required by applicable law or agreed to in writing, software distributed under the Licenses is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licenses for the specific language governing permissions and limitations under the Licenses.
