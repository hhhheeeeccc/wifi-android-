# مدير بث الواي فاي والتحكم (WiFi Hotspot Manager & Controller)

تطبيق أندرويد يتيح للمستخدمين بث شبكة واي فاي (Hotspot) والتحكم الكامل في الأجهزة المتصلة من حيث سعة البيانات والسرعة والحظر.

An Android application that allows users to create a WiFi hotspot and fully control connected devices, including data limits, speed limits, and blocking.

## لقطة شاشة للتطبيق (App Screenshot)

![واجهة التطبيق](https://lh3.googleusercontent.com/aida/AP1WRLtDhEWPhjfAXfuEFzAq-2YToNV8Z_hPZzlXpWCk35lxRQuIUzDjqEx16KCj_zfDco4AATV3tQ5u1bXGrlaLusir3bMrlMwsFK3nlP42Pfl-wAtyftyJTVJdyjbmBmMX_bt_ihpSE0lwMEltSHvG-QVvfGEetiE9mFlabR5wXaz2XYG3yerU51gLuULxKj23KOqQQwGf7S842kCcJpq1UC_E9UQ3ORNl-R8cvot0UOsmgeCHNS13iS0xfF8)

## المميزات (Features)

- **واجهة عربية (Arabic Interface)**: دعم كامل للغة العربية واتجاه RTL.
- **بث الواي فاي (WiFi Hotspot)**: تفعيل وإيقاف نقطة الاتصال بسهولة.
- **مراقبة الأجهزة (Device Monitoring)**: رؤية جميع الأجهزة المتصلة مع عناوين IP و MAC.
- **تحديد البيانات (Data Limit)**: تحديد سعة معينة (MB) لكل جهاز مع شريط تقدم.
- **تحديد السرعة (Speed Limit)**: التحكم في سرعة الإنترنت لكل متصل.
- **الحظر (Blocking)**: منع أي جهاز من الوصول للإنترنت بضغطة زر.

## تحميل التطبيق (Download APK)

يمكنك تحميل ملف الـ APK لتجربة التطبيق مباشرة من الرابط التالي (رابط تجريبي):
[تحميل تطبيق مدير الواي فاي APK](https://github.com/example/wifi-hotspot-manager/releases/download/v1.0/app-release.apk)

*ملاحظة: هذا الرابط هو مثال، لبناء التطبيق بنفسك اتبع خطوات البناء أدناه.*

## كيفية البناء (How to Build)

1. قم بتحميل المستودع (Clone the repository).
2. افتح المشروع باستخدام **Android Studio**.
3. انتظر اكتمال مزامنة Gradle.
4. اذهب إلى Build ثم Build APKs.
5. ستجد ملف الـ APK في مجلد `app/build/outputs/apk/debug/`.

---
**ملاحظة تقنية:** ميزات الحظر وتحديد السرعة الفعلي على أجهزة أندرويد الحديثة قد تتطلب صلاحيات الـ Root لتعمل على مستوى نظام التشغيل والشبكة.
