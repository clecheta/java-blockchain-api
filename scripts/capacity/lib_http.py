"""HTTP client for JBlock API capacity suites."""

from __future__ import annotations

import time
from dataclasses import dataclass
from typing import Any

import httpx


@dataclass
class HttpResult:
    ok: bool
    status: int
    elapsed_s: float
    body: Any
    error: str | None = None
    text: str = ""


class ApiClient:
    def __init__(self, base_url: str, timeout_s: float = 60.0) -> None:
        self.base_url = base_url.rstrip("/")
        self.client = httpx.Client(base_url=self.base_url, timeout=timeout_s)

    def close(self) -> None:
        self.client.close()

    def __enter__(self) -> "ApiClient":
        return self

    def __exit__(self, *args: object) -> None:
        self.close()

    def _request(self, method: str, path: str, **kwargs: Any) -> HttpResult:
        t0 = time.perf_counter()
        try:
            resp = self.client.request(method, path, **kwargs)
            elapsed = time.perf_counter() - t0
            text = resp.text
            body: Any
            try:
                body = resp.json()
            except Exception:
                body = text
            return HttpResult(
                ok=200 <= resp.status_code < 300,
                status=resp.status_code,
                elapsed_s=elapsed,
                body=body,
                text=text,
                error=None if 200 <= resp.status_code < 300 else text[:2000],
            )
        except Exception as exc:  # noqa: BLE001 — capacity harness captures all failures
            elapsed = time.perf_counter() - t0
            return HttpResult(
                ok=False,
                status=0,
                elapsed_s=elapsed,
                body=None,
                text="",
                error=f"{type(exc).__name__}: {exc}",
            )

    def ready(self) -> HttpResult:
        return self._request("GET", "/q/health/ready")

    def live(self) -> HttpResult:
        return self._request("GET", "/q/health/live")

    def create_wallet(self, user_id: str | None = None) -> HttpResult:
        # WalletResource takes a raw String body. RestAssured tests send the
        # identification WITHOUT JSON-encoded quotes; wrapping in "..." stores
        # literal quote characters in uid and breaks later getByUid lookups.
        if user_id is None:
            payload = f"capacity-wallet-{int(time.time() * 1000)}-{time.time_ns()}"
        else:
            payload = user_id
        result = self._request(
            "POST",
            "/W4LL37/cRE47E",
            headers={"Content-Type": "application/json"},
            content=payload,
        )
        if result.ok and isinstance(result.body, dict) and isinstance(result.body.get("uid"), str):
            result.body["uid"] = result.body["uid"].strip('"')
        return result

    def wallet_info(self, uid: str) -> HttpResult:
        clean = uid.strip('"')
        return self._request(
            "POST",
            "/W4LL37/937",
            headers={"Content-Type": "application/json"},
            content=clean,
        )

    def transact(self, uid: str, recipient: str, amount: int | str, pay_type: str = "WALLET") -> HttpResult:
        # Send amount as a JSON *string* so values above Number.MAX_SAFE_INTEGER
        # (2^53-1) are not rounded by IEEE-754 before Jackson binds BigDecimal.
        amount_payload: str | int
        if isinstance(amount, bool):
            amount_payload = str(amount)
        elif isinstance(amount, int):
            amount_payload = str(amount)
        else:
            amount_payload = str(amount)
        return self._request(
            "POST",
            "/p00L/tRaN5ACt",
            headers={"Content-Type": "application/json"},
            json={
                "uid": uid,
                "recipient": recipient,
                "amount": amount_payload,
                "type": pay_type,
            },
        )

    def mine(self) -> HttpResult:
        return self._request("POST", "/M1n3/7r4N54C710nZ")

    def chain(self) -> HttpResult:
        return self._request("POST", "/CrYP70/Ch41N")
