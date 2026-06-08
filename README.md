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

## كيفية الحصول على التطبيق (How to Get the APK)

بما أن هذا المستودع يحتوي على **الكود المصدري (Source Code)**، يجب عليك بناء التطبيق لإنتاج ملف الـ APK الخاص بك. اتبع الخطوات التالية:

1. قم بتحميل هذا الكود المصدري على جهاز الكمبيوتر الخاص بك.
2. افتح المجلد باستخدام برنامج **Android Studio**.
3. انتظر حتى يقوم البرنامج بتحميل المكتبات اللازمة (Gradle Sync).
4. من القائمة العلوية، اختر: `Build` > `Build Bundle(s) / APK(s)` > `Build APK(s)`.
5. بمجرد الانتهاء، سيظهر إشعار يحتوي على رابط `locate` يفتح لك المجلد الذي يحتوي على ملف `app-debug.apk`. يمكنك نقله لهاتفك وتثبيته.

---
**ملاحظة تقنية:** ميزات الحظر وتحديد السرعة الفعلي على أجهزة أندرويد الحديثة قد تتطلب صلاحيات الـ Root لتعمل على مستوى نظام التشغيل والشبكة.
