$SDK      = "C:\Users\CYJ\AppData\Local\Android\Sdk"
$EMULATOR = "$SDK\emulator\emulator.exe"
$ADB      = "$SDK\platform-tools\adb.exe"
$AVD      = "Galaxy_S25_API35"
$PKG      = "com.baekseok.shvoca"

# 1. 에뮬레이터가 이미 켜져 있는지 확인
$running = & $ADB devices 2>$null | Select-String "emulator"
if (-not $running) {
    Write-Host "[1/3] Galaxy S25 에뮬레이터 시작..."
    Start-Process $EMULATOR -ArgumentList "-avd $AVD"

    Write-Host "      부팅 대기 중 (최대 2분)..."
    $elapsed = 0
    do {
        Start-Sleep -Seconds 5
        $elapsed += 5
        $boot = & $ADB shell getprop sys.boot_completed 2>$null
    } while ($boot.Trim() -ne "1" -and $elapsed -lt 120)

    if ($boot.Trim() -ne "1") {
        Write-Host "부팅 시간 초과. 에뮬레이터 창을 직접 확인하세요." -ForegroundColor Red
        exit 1
    }
    Write-Host "      부팅 완료!" -ForegroundColor Green
} else {
    Write-Host "[1/3] 에뮬레이터 이미 실행 중 - 건너뜀"
}

# 2. 빌드 & 설치
Write-Host "[2/3] 빌드 & 설치 중..."
& ".\gradlew.bat" installDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "빌드 실패." -ForegroundColor Red
    exit 1
}

# 3. 앱 실행
Write-Host "[3/3] 앱 실행 중..."
& $ADB shell am start -n "$PKG/.MainActivity"
Write-Host "완료!" -ForegroundColor Green
