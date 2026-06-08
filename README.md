# مدير بث الواي فاي والتحكم (WiFi Hotspot Manager) - React Native TypeScript

تطبيق أندرويد متطور مبني باستخدام **React Native** ولغة **TypeScript** لضمان أفضل تجربة مستخدم مع كود برمي نظيف وقوي.

An advanced Android application built with **React Native** and **TypeScript**, ensuring a great UX with clean and robust code.

## التقنيات المستخدمة (Tech Stack)

- **React Native**: لبناء واجهة المستخدم عبر المنصات.
- **TypeScript**: لضمان فحص الأنواع الصارم وتقليل الأخطاء البرمجية.
- **Native Modules (Java)**: للتحكم المباشر في موارد نظام أندرويد (واي فاي، نقطة اتصال).
- **Hooks & State Management**: لإدارة حالة التطبيق بشكل حديث (MVVM style).

## المميزات (Features)

- **دعم كامل للغة العربية**: واجهة مستخدم مصممة خصيصاً لتدعم RTL.
- **تحكم كامل في البث**: ضبط الاسم والرمز بضغطة زر.
- **وضع البروكسي (Proxy Mode)**: مشاركة الإنترنت في الأجهزة الحديثة التي لا تحتوي على Root.
- **مراقبة المتصلين**: عرض الأجهزة المتصلة مع خيارات الحظر وتحديد السرعة والبيانات.

## هيكلية المشروع (Project Structure)

- `App.tsx`: المكون الرئيسي للتطبيق.
- `src/models/`: تعريف أنواع البيانات والواجهات.
- `src/viewmodels/`: إدارة منطق الواجهة (Hooks).
- `src/components/`: مكونات الواجهة القابلة لإعادة الاستخدام.
- `app/src/main/java/...`: كود أندرويد الأصلي للتحكم في النظام.

## كيفية التشغيل (Getting Started)

1. تأكد من تثبيت بيئة React Native على جهازك.
2. قم بتثبيت التبعيات:
   ```bash
   npm install
   ```
3. تشغيل التطبيق على محاكي أو جهاز حقيقي:
   ```bash
   npx react-native run-android
   ```

---
تم تطوير هذا المشروع ليكون مثالاً يحتذى به في استخدام TypeScript مع React Native للتحكم في خصائص النظام المتقدمة.
