# مدير بث الواي فاي (WiFi Hotspot Manager) - React Native Version

تطبيق أندرويد تم تحويله إلى **React Native** لتقديم أفضل تجربة مستخدم مع الحفاظ على الأداء العالي والتحكم في موارد النظام.

An Android application converted to **React Native** for a better user experience and high-performance system control.

## المميزات (Features)

- **واجهة عصرية**: مبنية باستخدام React Native مع دعم كامل للغة العربية واتجاه RTL.
- **Native Bridge**: ربط الكود البرمجي بـ Native Modules للتحكم المباشر في إعدادات الواي فاي.
- **إدارة كاملة**: تفعيل البث، تخصيص الاسم والرمز، ومراقبة الأجهزة المتصلة.
- **دعم كافة الحالات**: التعامل مع الهواتف التي تحتوي على Root والتي لا تحتوي عليه.

## لقطة شاشة (Screenshot)
![واجهة التطبيق](https://lh3.googleusercontent.com/aida/AP1WRLtDhEWPhjfAXfuEFzAq-2YToNV8Z_hPZzlXpWCk35lxRQuIUzDjqEx16KCj_zfDco4AATV3tQ5u1bXGrlaLusir3bMrlMwsFK3nlP42Pfl-wAtyftyJTVJdyjbmBmMX_bt_ihpSE0lwMEltSHvG-QVvfGEetiE9mFlabR5wXaz2XYG3yerU51gLuULxKj23KOqQQwGf7S842kCcJpq1UC_E9UQ3ORNl-R8cvot0UOsmgeCHNS13iS0xfF8)

## كيفية التشغيل (How to Run)

1. قم بتثبيت التبعيات:
   ```bash
   npm install
   ```
2. تشغيل التطبيق على الأندرويد:
   ```bash
   npx react-native run-android
   ```

## هيكلية المشروع (Architecture)

- `App.js`: الملف الرئيسي للواجهة والمنطق.
- `android/app/src/main/java/...`: تحتوي على الـ Native Modules للتحكم في الواي فاي.
- `package.json`: تكوين المشروع والتبعيات.

---
تم التحويل إلى React Native لتسهيل التطوير المستقبلي وتحسين سرعة الاستجابة.
