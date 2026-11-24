# BlackDemon — Android VPN клиент (Kotlin + Jetpack Compose)

## Как собрать
1. Установите Android Studio (Koala или новее, JDK 17 идёт в комплекте).
2. Скачайте ядро Xray для Android — файл `libv2ray.aar`:
   релизы https://github.com/2dust/AndroidLibXrayLite/releases (либо возьмите из сборки v2rayNG).
   Положите в `app/libs/libv2ray.aar`.
3. File → Open → папка `BlackDemon`, дождитесь Gradle Sync.
4. Debug-APK: Build → Build APK(s) → `app/build/outputs/apk/debug/app-debug.apk`
5. Релиз: Build → Generate Signed Bundle / APK → APK → создайте keystore → release.
6. Установите APK на телефон (разрешите установку из неизвестных источников).

## Подписка
Базовый адрес: `Config.SUB_HOST` в `data/Data.kt` (https://sub.layersync.online).
Добавляется тремя способами: вставить ссылку/токен на вкладке «Подписка»; скопировать ссылку
(приложение подхватит из буфера при открытии); открыть `blackdemon://import/<url-encoded ссылка>`.
Кнопка «корзина» открывает `Config.SHOP_URL`.

## Картинки
Все в `app/src/main/res/drawable` (векторные): ic_demon (логотип), ic_launcher_*, иконки меню.
