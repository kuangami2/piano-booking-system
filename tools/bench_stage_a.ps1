# Stage A benchmark script
# Run: powershell -ExecutionPolicy Bypass -File bench_stage_a.ps1
$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:9090'
$rounds = 200

$login = Invoke-RestMethod -Uri "$base/api/auth/login" -Method Post -ContentType 'application/json' -Body '{"username":"demo","password":"123456"}'
$h = @{ token = $login.data.token }
$tomorrow = (Get-Date).AddDays(1).ToString('yyyy-MM-dd')

function Bench {
    param($Name, $Uri)
    $times = New-Object System.Collections.Generic.List[double]
    for ($i = 0; $i -lt $rounds; $i++) {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        Invoke-RestMethod -Uri $Uri -Headers $h | Out-Null
        $sw.Stop()
        $times.Add($sw.Elapsed.TotalMilliseconds)
    }
    $sorted = $times | Sort-Object
    $p50 = $sorted[[int]($rounds * 0.50)]
    $p95 = $sorted[[int]($rounds * 0.95)]
    $p99 = $sorted[[int]([math]::Min($rounds - 1, [int]($rounds * 0.99)))]
    Write-Host ("{0}: P50={1:N1}ms P95={2:N1}ms P99={3:N1}ms avg={4:N1}ms" -f $Name, $p50, $p95, $p99, ($times | Measure-Object -Average).Average)
}

Bench 'rooms list' "$base/api/rooms"
Bench 'free slots' "$base/api/rooms/3/free?date=$tomorrow"
Write-Host 'done. Record the numbers in docs/performance notes for before/after comparison.'
