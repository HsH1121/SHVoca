# 한자 100 (SHVOCA)

K-MOVE 일본 소프트웨어 양성과정 3차 중간평가 대비 한자 100개 플래시카드 앱.
Jetpack Compose 네이티브 안드로이드 앱.

## 실행 방법

1. **압축 해제** 후 폴더(`SHVOCA`)를 통째로 보관
2. **Android Studio** 실행 → `File > Open` → `SHVOCA` 폴더 선택
3. 처음 열면 Gradle이 자동으로 의존성을 다운로드함 (인터넷 필요, 5~10분)
4. 상단 초록 ▶ 버튼(Run) → 에뮬레이터 또는 USB 연결한 폰에서 실행

> 실제 폰에서 돌리려면: 폰 `설정 > 개발자 옵션 > USB 디버깅` 켜고 USB 연결.
> 개발자 옵션은 `설정 > 휴대전화 정보 > 빌드번호` 7번 탭하면 켜짐.

## 기능

- 앞면 한자 → 탭하면 후리가나 + 뜻 (3D 플립 애니메이션)
- 좌우 스와이프 또는 이전/다음 버튼으로 이동
- 진행바로 현재 위치(1/100) 표시
- 🔀 섞기 / ↕ 원래순서
- 카드마다 원본 번호(No.) 표시

## 폴더 구조

```
app/src/main/java/com/baekseok/shvoca/
├─ MainActivity.kt              앱 진입점
├─ data/
│  └─ KanjiWord.kt              단어 100개 데이터
└─ ui/
   ├─ KanjiCardScreen.kt        화면 + 플립카드 로직
   └─ theme/                    색상·테마
```

단어를 수정하려면 `data/KanjiWord.kt`의 리스트만 고치면 됨.

## 버전

- Kotlin 2.0.21 / AGP 8.7.3 / Gradle 8.9
- Compose BOM 2024.10.01
- minSdk 24 (Android 7.0+) / targetSdk 35

## 정정 내역

- 99번: 忠実 → **充実(じゅうじつ)** 교체
- 표준 발음 통일: 永遠 えいえん, 工夫 くふう, 在日 ざいにち 등
