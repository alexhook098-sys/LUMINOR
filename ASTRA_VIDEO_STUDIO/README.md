# ASTRA VIDEO STUDIO

Самостоятельный Android-модуль в репозитории LUMINOR. Приложение на Kotlin + Jetpack Compose, развивающее сценарий Photo Sequence.

## UX
- Крупные сенсорные элементы и понятные подписи.
- Массовый импорт изображений.
- Настройка длительности кадров (по умолчанию 0,10 с).
- Инструменты сгруппированы по задачам; функции не выдаются за готовые, пока не реализованы.

## Текущий статус
Это исходный стартовый каркас, не готовый APK. Сейчас доступны интерфейс, импорт изображений, удаление кадров и расчёт длительности. Рендер MP4, drag-and-drop, FFmpeg, RIFE и Optical Flow ещё не подключены.

## Сборка
GitHub Actions workflow `.github/workflows/astra-video-studio-build.yml` собирает debug APK на GitHub-hosted runner (JDK 17, Gradle 8.9, Android SDK 35) и публикует артефакт `astra-video-studio-debug-apk` на 14 дней. Сборка считается проверенной только после успешного завершения workflow.

## Целевое устройство
Samsung SM-A127F / Exynos 850 / Mali. Vulkan и RIFE должны пройти отдельную проверку на реальном GPU; Adreno-сборки не считаются совместимыми с Mali автоматически.

## Лицензирование
См. `LICENSE_POLICY.md`, `THIRD_PARTY_LICENSES.md` и `licenses/ASTRA_MODULE_REGISTRY.json`. Компоненты с непроверенными лицензиями не включаются в коммерческий релиз.
