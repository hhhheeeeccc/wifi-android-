# مدير بث الواي فاي والتحكم (WiFi Hotspot Manager) - React Native TypeScript Edition (Direct Edition)

تطبيق متكامل للأندرويد و iOS مبني باستخدام **React Native** و **TypeScript**، يتبع نمط **MVVM** لتقديم أفضل تجربة مستخدم وتحكم كامل في موارد النظام باللغة العربية.

A cross-platform (Android/iOS) application built with **React Native** and **TypeScript**, following the **MVVM pattern**.

## المميزات (Features)

- **دعم TypeScript الكامل**: لضمان استقرار الكود وفحص الأنواع.
- **واجهة مستخدم احترافية (RTL)**: تصميم عصري (Direct Edition) يدعم اللغة العربية بالكامل.
- **تفعيل ذكي للبث**:
  - **أندرويد (Root)**: تفعيل صامت ومباشر باستخدام أوامر `cmd tethering`.
  - **أندرويد (Reflection)**: محاولة التفعيل المباشر للأجهزة بدون روت عبر تقنيات الانعكاس البرمجي.
  - **أندرويد (LocalOnly)**: تفعيل البث للمشاركة المحلية كخيار بديل.
  - **iOS**: دعم التوجيه المباشر لإعدادات نقطة الاتصال الشخصية.
- **إدارة المتصلين**: مراقبة الأجهزة المتصلة، حظر الأجهزة، وتحديد السرعة.

## هيكلية المشروع (Architecture)

تم تقسيم المشروع لاتباع أفضل الممارسات:
- `App.tsx`: الواجهة الرئيسية بتصميم Direct Edition المطور.
- `src/models/`: تعريف أنواع البيانات والواجهات (TypeScript Interfaces).
- `src/viewmodels/`: إدارة منطق التطبيق وحالته (Custom Hooks).
- `src/components/`: مكونات الواجهة القابلة لإعادة الاستخدام (`DeviceCard`).
- `android/`: Native Modules للأندرويد (Java).
- `ios/`: Native Modules للـ iOS (Swift/Obj-C).

## المتطلبات والتشغيل (Setup)

1. تأكد من تثبيت **Node.js** و **Android Studio** و **Xcode**.
2. تثبيت التبعيات:

3. تشغيل التطبيق (أندرويد):

4. تشغيل التطبيق (iOS):


---
تم تطوير هذا المشروع ليكون تطبيقاً احترافياً (Direct Edition) يحل مشاكل قيود النظام في تفعيل بث الشبكة.
