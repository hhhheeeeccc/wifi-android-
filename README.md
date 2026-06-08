# مدير بث الواي فاي والتحكم (WiFi Hotspot Manager) - React Native TypeScript Edition

تطبيق أندرويد متكامل مبني باستخدام **React Native** و **TypeScript**، يتبع نمط **MVVM** لتقديم أفضل تجربة مستخدم وتحكم كامل في موارد النظام باللغة العربية.

An Android application built with **React Native** and **TypeScript**, following the **MVVM pattern** for a professional and robust architecture.

## المميزات (Features)

- **دعم TypeScript الكامل**: لضمان استقرار الكود وفحص الأنواع.
- **واجهة مستخدم عربية (RTL)**: تجربة مستخدم سلسة ومصممة خصيصاً للمستخدم العربي.
- **Native Bridge (Java)**: التحكم المباشر في إعدادات النظام (الواي فاي، نقطة الاتصال) عبر جسر برمي قوي.
- **إدارة متطورة (Root & Proxy)**:
  - تفعيل تلقائي في حالة وجود Root.
  - وضع البروكسي (Proxy Mode) للأجهزة الحديثة بدون Root لمشاركة الإنترنت.
- **إدارة المتصلين**: حظر الأجهزة، تحديد السرعة، وتحديد سعة البيانات (ميجا).

## هيكلية المشروع (Architecture)

تم تقسيم المشروع لاتباع أفضل الممارسات:
- `App.tsx`: المكون الرئيسي للواجهة.
- `src/models/`: تعريف أنواع البيانات والواجهات (TypeScript Interfaces).
- `src/viewmodels/`: إدارة منطق التطبيق وحالته (Custom Hooks).
- `src/components/`: مكونات الواجهة القابلة لإعادة الاستخدام.
- `android/`: يحتوي على الكود الأصلي (Java) والـ Native Modules.

## المتطلبات والتشغيل (Setup)

1. تأكد من تثبيت **Node.js** و **Android Studio**.
2. تثبيت التبعيات:
   ```bash
   npm install
   ```
3. تشغيل التطبيق:
   ```bash
   npx react-native run-android
   ```

---
تم تطوير هذا المشروع ليكون تطبيقاً احترافياً يجمع بين سرعة React Native وقوة Android الأصلية.
