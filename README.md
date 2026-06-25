# SHVOCA

단어장을 직접 만들고 단어를 자유롭게 추가·관리하는 플래시카드 암기 앱.
일본어·영어·중국어·한자 등 언어 종류에 상관없이 쓸 수 있다.
Jetpack Compose 네이티브 안드로이드 앱.

## 기능

- **단어장 관리** — 단어장 추가·수정·삭제 (언어 태그: 일본어·영어·중국어·한자)
- **단어 추가** — 단어장마다 단어 자유 추가 (원문·발음·뜻·간번체 등 언어별 맞춤 입력)
- **플립카드 학습** — 앞면(원문) 탭 → 발음·뜻 3D 플립 애니메이션
- **스와이프 이동** — 좌우 스와이프 또는 이전/다음 버튼
- **셔플·원래순서** — 카드 순서 섞기 및 복원
- **가리기 모드** — 단어 목록에서 원문 또는 뜻을 가리고 탭해서 개별 공개
- **북마크** — 단어마다 ★ 표시, 북마크 필터로 모아보기
- **메모** — 카드 뒷면에 메모 저장
- **테스트** — 후리가나 직접 입력(서답형) / 뜻 고르기(6지 선다형), 틀린 단어 결과 표시

## 실행 방법

1. **Android Studio** 실행 → `File > Open` → 이 폴더 선택
2. 처음 열면 Gradle이 자동으로 의존성 다운로드 (인터넷 필요, 5~10분)
3. 상단 초록 ▶ Run → 에뮬레이터 또는 USB 연결한 폰에서 실행

> 실제 폰 실행: `설정 > 개발자 옵션 > USB 디버깅` 켜고 USB 연결.
> 개발자 옵션은 `설정 > 휴대전화 정보 > 빌드번호` 7번 탭하면 활성화.

## 폴더 구조

```
app/src/main/java/com/baekseok/shvoca/
├─ MainActivity.kt
├─ data/
│  ├─ KanjiWord.kt          단어 엔티티
│  ├─ VocabBook.kt          단어장 엔티티
│  ├─ KanjiDao.kt           Room DAO
│  └─ KanjiDatabase.kt      DB 싱글톤 · 마이그레이션 · 초기 데이터
└─ ui/
   ├─ LanguageSelectScreen.kt   단어장 목록
   ├─ WordListScreen.kt         단어 목록
   ├─ KanjiCardScreen.kt        플립카드
   ├─ TestScreen.kt             테스트
   └─ theme/                    색상·테마
```

## 버전

- Kotlin 2.0.21 / AGP 8.7.3 / Gradle 8.9
- Compose BOM 2024.10.01
- minSdk 33 (Android 13+) / targetSdk 35
