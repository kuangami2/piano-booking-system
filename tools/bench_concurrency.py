#!/usr/bin/env python3
"""并发压测：验证 50 与 100 并发下读接口的错误率与响应时间，并可选验证同一时段并发预约的唯一性。

用法（在 WSL 或本机 python3 下运行）：
    python3 tools/bench_concurrency.py
    python3 tools/bench_concurrency.py --host 127.0.0.1:9090 --rounds 3
    python3 tools/bench_concurrency.py --booking

读场景取琴房列表接口，每线程独立连接以复用连接，统计成功数与 P50、P95、P99、QPS。
预约场景用 10 并发同时抢同一时段，预期仅 1 条成功、其余返回 409，用于验证行锁加唯一约束。
"""
import argparse
import http.client
import json
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor

# Windows 控制台默认 GBK，统一按 UTF-8 输出中文
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

TODAY_PLACEHOLDER = None


def request(conn, method, path, body=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["token"] = token
    payload = json.dumps(body).encode("utf-8") if body is not None else None
    start = time.perf_counter()
    conn.request(method, path, payload, headers)
    resp = conn.getresponse()
    raw = resp.read()
    cost = (time.perf_counter() - start) * 1000
    try:
        data = json.loads(raw.decode("utf-8"))
    except Exception:
        data = {"code": "parse-error", "msg": raw.decode("utf-8", "ignore")[:80]}
    return cost, data


def login(host, port, user, password):
    conn = http.client.HTTPConnection(host, port, timeout=10)
    _, data = request(conn, "POST", "/api/auth/login", {"username": user, "password": password})
    conn.close()
    if str(data.get("code")) != "200":
        raise SystemExit("登录失败：%s" % data.get("msg"))
    return data["data"]["token"]


def percentile(values, ratio):
    if not values:
        return 0.0
    ordered = sorted(values)
    index = min(len(ordered) - 1, int(len(ordered) * ratio))
    return ordered[index]


def warmup(host, port, token, times=5):
    """预热热点缓存，避免首轮冷缓存把等待首个回源的时间算进指标"""
    conn = http.client.HTTPConnection(host, port, timeout=10)
    for _ in range(times):
        request(conn, "GET", "/api/rooms?page=1&size=10", token=token)
    conn.close()


def bench_read(host, port, token, concurrency, rounds):
    samples = []
    lock = threading.Lock()

    def worker(_):
        local = []
        conn = http.client.HTTPConnection(host, port, timeout=20)
        for _ in range(rounds):
            try:
                cost, data = request(conn, "GET", "/api/rooms?page=1&size=10", token=token)
                local.append((cost, str(data.get("code")) == "200", data.get("msg") if str(data.get("code")) != "200" else ""))
            except Exception as exc:
                local.append((0.0, False, "连接异常:%s" % exc))
                try:
                    conn.close()
                except Exception:
                    pass
                conn = http.client.HTTPConnection(host, port, timeout=20)
        conn.close()
        with lock:
            samples.extend(local)

    start = time.perf_counter()
    with ThreadPoolExecutor(max_workers=concurrency) as pool:
        list(pool.map(worker, range(concurrency)))
    elapsed = time.perf_counter() - start

    costs = [c for c, ok, _ in samples]
    fails = [(c, msg) for c, ok, msg in samples if not ok]
    total = len(samples)
    print("并发 %-4d 请求 %-5d 成功 %-5d 失败 %-4d 错误率 %5.2f%%  QPS %7.1f" % (
        concurrency, total, total - len(fails), len(fails),
        len(fails) * 100.0 / max(1, total), total / max(elapsed, 1e-6)))
    print("            P50 %6.1fms  P95 %6.1fms  P99 %6.1fms  最大 %6.1fms" % (
        percentile(costs, 0.50), percentile(costs, 0.95), percentile(costs, 0.99), max(costs) if costs else 0))
    if fails:
        detail = {}
        for _, msg in fails:
            detail[msg] = detail.get(msg, 0) + 1
        print("            失败分布:", detail)
    return len(fails), total


def bench_booking(host, port, token, concurrency, book_date, room_id, start_min, end_min):
    results = []
    lock = threading.Lock()
    barrier = threading.Barrier(concurrency)

    def worker(_):
        conn = http.client.HTTPConnection(host, port, timeout=20)
        barrier.wait()
        try:
            _, data = request(conn, "POST", "/api/bookings",
                              {"roomId": room_id, "bookDate": book_date,
                               "startMin": start_min, "endMin": end_min}, token=token)
            with lock:
                results.append((str(data.get("code")), data.get("msg")))
        except Exception as exc:
            with lock:
                results.append(("exception", str(exc)))
        conn.close()

    with ThreadPoolExecutor(max_workers=concurrency) as pool:
        list(pool.map(worker, range(concurrency)))

    summary = {}
    for code, _ in results:
        summary[code] = summary.get(code, 0) + 1
    print("并发预约 %d 次同一时段，返回分布：%s" % (concurrency, summary))
    print("预期：200 为 1 条，其余为 409 冲突，用于验证行锁与唯一约束；限额为每用户每分钟 %s 次" % "10")
    return summary


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="172.25.240.1:9090", help="后端地址，WSL 用宿主 IP，本机用 127.0.0.1:9090")
    parser.add_argument("--concurrency", default="50,100", help="逗号分隔的并发数")
    parser.add_argument("--rounds", type=int, default=2, help="每线程请求次数")
    parser.add_argument("--user", default="demo")
    parser.add_argument("--password", default="123456")
    parser.add_argument("--booking", action="store_true", help="改为验证同一时段并发预约唯一性")
    parser.add_argument("--date", default=time.strftime("%Y-%m-%d"))
    parser.add_argument("--room-id", type=int, default=4)
    parser.add_argument("--start", type=int, default=1080)
    parser.add_argument("--end", type=int, default=1110)
    args = parser.parse_args()

    host, _, port = args.host.partition(":")
    port = int(port or 9090)
    token = login(host, port, args.user, args.password)
    print("压测目标 http://%s:%d，账号 %s" % (host, port, args.user))

    if args.booking:
        bench_booking(host, port, token, 10, args.date, args.room_id, args.start, args.end)
        return

    total_fail = 0
    warmup(host, port, token)
    print("已预热热点缓存")
    for concurrency in [int(x) for x in args.concurrency.split(",") if x.strip()]:
        fail, _ = bench_read(host, port, token, concurrency, args.rounds)
        total_fail += fail
    print("结论：%s" % ("未出现失败，连接池与缓存表现正常" if total_fail == 0 else "存在失败请求，见上方分布"))


main()
