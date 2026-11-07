"""Create and fund wallets through the production API contract."""

from __future__ import annotations

import os
import time
from typing import Any

import httpx


def seed_funded_wallets(
    count: int,
    balance: int | str,
    timeout_s: float = 300.0,
) -> list[dict[str, Any]]:
    """Create and idempotently fund wallets via HTTP against PostgreSQL."""
    base_url = os.environ.get("ROOT_URL", "http://localhost:8018").rstrip("/")
    client_api_key = os.environ.get(
        "BANK_CLIENT_API_KEY", "local-client-only-change-me")
    service_api_key = os.environ.get(
        "BANK_SERVICE_API_KEY", "local-development-only-change-me")
    wallets: list[dict[str, Any]] = []
    with httpx.Client(base_url=base_url, timeout=timeout_s) as client:
        for index in range(count):
            uid = f"capacity-funded-{time.time_ns()}-{index}"
            created = client.post(
                "/W4LL37/cRE47E",
                headers={
                    "Content-Type": "application/json",
                    "X-API-Key": client_api_key,
                },
                content=uid,
            )
            created.raise_for_status()
            wallet = created.json()
            wallet["uid"] = str(wallet["uid"]).strip('"')
            funded = client.post(
                "/W4LL37/fUND",
                headers={"X-API-Key": service_api_key},
                json={
                    "uid": wallet["uid"],
                    "amount": str(balance),
                    "idempotencyKey": f"capacity-{wallet['uid']}",
                },
            )
            funded.raise_for_status()
            wallet["balance"] = str(balance)
            wallets.append(wallet)
    return wallets
