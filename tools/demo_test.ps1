# Room Reservation acceptance demo script
# Run: powershell -ExecutionPolicy Bypass -File demo_test.ps1
# Backend must be running at 127.0.0.1:9090 with seeded data.
$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:9090'
$pass = 0
$fail = 0

function Call-Api {
    param($Method, $Path, $Body, $Token)
    $headers = @{}
    if ($Token) { $headers.token = $Token }
    $params = @{ Uri = ($base + $Path); Method = $Method; Headers = $headers; ContentType = 'application/json' }
    if ($null -ne $Body) { $params.Body = ConvertTo-Json -InputObject $Body -Compress }
    $resp = Invoke-RestMethod @params
    if ($resp.code -ne '200') { throw ($Path + ' -> code ' + $resp.code + ' ' + $resp.msg) }
    return $resp
}

# returns 1 when the call is rejected (non 200), 0 when it succeeds
function Expect-Rejected {
    param($Method, $Path, $Body, $Token)
    try {
        Call-Api -Method $Method -Path $Path -Body $Body -Token $Token | Out-Null
        return 0
    } catch {
        return 1
    }
}

function Check {
    param($Name, $Cond)
    if ($Cond) { Write-Host "[PASS] $Name"; $script:pass++ }
    else { Write-Host "[FAIL] $Name"; $script:fail++ }
}

$stamp = Get-Date -Format 'HHmmss'
$upwd = 'pass123'
$u1 = 'u1' + $stamp
$u2 = 'u2' + $stamp
$tomorrow = (Get-Date).AddDays(1).ToString('yyyy-MM-dd')

Write-Host '== Scene 1: register, login, change password =='
Call-Api -Method Post -Path '/api/auth/register' -Body @{username = $u1; password = $upwd; name = 'User One'; studentNo = 'T1' + $stamp; email = ($u1 + '@t.local')} | Out-Null
Check 'register u1' $true
Call-Api -Method Post -Path '/api/auth/register' -Body @{username = $u2; password = $upwd; name = 'User Two'; studentNo = 'T2' + $stamp; email = ($u2 + '@t.local')} | Out-Null
Check 'register u2' $true
$loginU2 = Call-Api -Method Post -Path '/api/auth/login' -Body @{username = $u2; password = $upwd}
$t2 = $loginU2.data.token
Check 'login u2 returns token' (-not [string]::IsNullOrEmpty($t2))
$rejected = Expect-Rejected -Method Post -Path '/api/auth/register' -Body @{username = $u1; password = $upwd; name = 'Dup'; studentNo = 'X9'; email = 'x@t.local'}
Check 'duplicate username rejected' ($rejected -eq 1)

$login = Call-Api -Method Post -Path '/api/auth/login' -Body @{username = $u1; password = $upwd}
$t1 = $login.data.token
Check 'login u1 returns token' (-not [string]::IsNullOrEmpty($t1))

Call-Api -Method Put -Path '/api/auth/password' -Body @{password = $upwd; newPassword = 'new123'} -Token $t1 | Out-Null
$login2 = Call-Api -Method Post -Path '/api/auth/login' -Body @{username = $u1; password = 'new123'}
$t1 = $login2.data.token
Check 'login with new password' (-not [string]::IsNullOrEmpty($t1))
Call-Api -Method Put -Path '/api/auth/password' -Body @{password = 'new123'; newPassword = $upwd} -Token $t1 | Out-Null
Check 'change password back' $true
$login3 = Call-Api -Method Post -Path '/api/auth/login' -Body @{username = $u1; password = $upwd}
if (-not $login3.data -or [string]::IsNullOrEmpty($login3.data.token)) { Write-Host ('DEBUG login3 code=' + $login3.code + ' dataNull=' + ($null -eq $login3.data)) }
$t1 = $login3.data.token
Check 'relogin after revert' (-not [string]::IsNullOrEmpty($t1))

Write-Host '== Scene 2: room browse, search, inner/outer =='
$rooms = Call-Api -Method Get -Path '/api/rooms' -Token $t1
Check 'normal user sees only outer rooms (3)' ($rooms.data.total -eq 3)
$search = Call-Api -Method Get -Path '/api/rooms?q=B101' -Token $t1
Check 'keyword search finds B101' ($search.data.total -ge 1)
$rejected = Expect-Rejected -Method Get -Path '/api/rooms/1' -Token $t1
Check 'inner room detail rejected for normal user' ($rejected -eq 1)
$free = Call-Api -Method Get -Path ("/api/rooms/3/free?date=" + $tomorrow) -Token $t1
Check 'free slots returned' ($free.data.Count -ge 1)

Write-Host '== Scene 3: booking success and conflict =='
$slot = @{roomId = 3; bookDate = $tomorrow; startMin = 600; endMin = 840}
Call-Api -Method Post -Path '/api/bookings' -Body $slot -Token $t1 | Out-Null
Check 'booking created' $true
$rejected = Expect-Rejected -Method Post -Path '/api/bookings' -Body $slot -Token $t2
Check 'overlap conflict rejected' ($rejected -eq 1)
$rejected = Expect-Rejected -Method Post -Path '/api/bookings' -Body @{roomId = 1; bookDate = $tomorrow; startMin = 600; endMin = 840} -Token $t1
Check 'inner room booking rejected' ($rejected -eq 1)
$mine = Call-Api -Method Get -Path '/api/bookings/mine?status=booked' -Token $t1
Check 'my booking listed' ($mine.data.total -ge 1)

Write-Host '== Scene 4: watch and vacancy message after cancel =='
Call-Api -Method Post -Path '/api/watches' -Body @{roomId = 3; bookDate = $tomorrow; startMin = 600; endMin = 840} -Token $t2 | Out-Null
Check 'u2 watches the slot' $true
$mine = Call-Api -Method Get -Path '/api/bookings/mine?status=booked' -Token $t1
$bid = $mine.data.list[0].id
Call-Api -Method Delete -Path ('/api/bookings/' + $bid) -Token $t1 | Out-Null
Check 'u1 cancels booking' $true
Start-Sleep -Seconds 2
$msgs = Call-Api -Method Get -Path '/api/messages?unread=1' -Token $t2
Check 'u2 receives vacancy message' ($msgs.data.total -ge 1)

Write-Host '== Scene 5: admin credit deduct suspends booking =='
$me = Call-Api -Method Get -Path '/api/auth/profile' -Token $t1
$uid1 = $me.data.id
$adminLogin = Call-Api -Method Post -Path '/api/auth/login' -Body @{username = 'admin'; password = 'admin123'}
$ta = $adminLogin.data.token
Check 'admin login' (-not [string]::IsNullOrEmpty($ta))
$ulist = Call-Api -Method Get -Path ('/api/admin/users?keyword=' + $u1) -Token $ta
$creditBefore = [int]$ulist.data.list[0].credit
$res = Call-Api -Method Put -Path ('/api/admin/credits/' + $uid1 + '/deduct') -Body @{points = 50; reason = 'demo 减分暂停'} -Token $ta
Check 'deduct 50 credits' ($res.data.credit -eq ($creditBefore - 50))
Check 'credit paused below threshold' ($res.data.paused -eq $true)
$rejected = Expect-Rejected -Method Post -Path '/api/bookings' -Body @{roomId = 3; bookDate = $tomorrow; startMin = 960; endMin = 1020} -Token $t1
Check 'booking blocked while suspended' ($rejected -eq 1)
$res = Call-Api -Method Put -Path ('/api/admin/credits/' + $uid1 + '/restore') -Body @{points = 50; reason = 'demo 恢复'} -Token $ta
Check 'restore credit to max 100' ($res.data.credit -eq 100)

Write-Host '== Scene 6: rules live update takes effect =='
$rules = Call-Api -Method Get -Path '/api/admin/rules' -Token $ta
Check 'admin reads 18 rule params' ($rules.data.Count -eq 18)
$orig = ($rules.data | Where-Object { $_.ruleKey -eq 'booking.maxDurationMin' }).ruleValue
$payload = @(@{key = 'booking.maxDurationMin'; value = '30'})
Call-Api -Method Put -Path '/api/admin/rules' -Body $payload -Token $ta | Out-Null
$rejected = Expect-Rejected -Method Post -Path '/api/bookings' -Body @{roomId = 3; bookDate = $tomorrow; startMin = 600; endMin = 660} -Token $t1
Check '60-min booking rejected after limit change' ($rejected -eq 1)
$payloadRestore = @(@{key = 'booking.maxDurationMin'; value = [string]$orig})
Call-Api -Method Put -Path '/api/admin/rules' -Body $payloadRestore -Token $ta | Out-Null
$rulesAfter = Call-Api -Method Get -Path '/api/admin/rules' -Token $ta
$now = ($rulesAfter.data | Where-Object { $_.ruleKey -eq 'booking.maxDurationMin' }).ruleValue
Check 'rule value restored' ($now -eq $orig)

Write-Host ("RESULT pass=" + $pass + " fail=" + $fail)
