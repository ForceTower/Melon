import { describe, expect, test } from "bun:test";
import { createMockHandler } from "./mock-melon";
import { pilot } from "./scenarios";

function request(path: string, method: "GET" | "POST" = "GET", body?: unknown) {
  const headers = {
    authorization: "Bearer synthetic-access-0",
    "content-type": "application/json",
  };
  if (method === "GET") return new Request(`http://localhost${path}`, { method, headers });
  return new Request(`http://localhost${path}`, {
    method: "POST",
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

describe("hermetic scenario server", () => {
  test("synthetic login and complete initial sync need no upstream", async () => {
    const handle = createMockHandler();
    const login = await handle(request("/api/auth/login", "POST", pilot.account));
    expect(await login.json()).toMatchObject({ ok: true, data: pilot.login });
    for (const [path, expected] of [
      ["/api/sync/profile", pilot.profile],
      ["/api/sync/onboarding-status", pilot.onboarding],
      [`/api/sync/semesters/${pilot.semester.semester.id}`, pilot.semester],
      ["/api/sync/messages", pilot.messages],
      ["/api/sync/events", pilot.events],
      ["/api/me/credentials", { credentials: pilot.account }],
    ] as const) {
      expect(await (await handle(request(path))).json()).toEqual({
        ok: true,
        message: null,
        data: expected,
      });
    }
    expect((await handle(request("/api/not-in-the-contract"))).status).toBe(501);
    expect(await (await handle(request("/debug/state"))).json()).toMatchObject({
      data: {
        mode: "hermetic",
        unexpectedRequests: ["GET /api/not-in-the-contract"],
      },
    });
  });

  test("rejects real-looking login credentials and unauthenticated data reads", async () => {
    const handle = createMockHandler();
    expect(
      (await handle(request("/api/auth/login", "POST", { username: "someone", password: "wrong" })))
        .status,
    ).toBe(400);
    expect((await handle(new Request("http://localhost/api/sync/profile"))).status).toBe(401);
  });

  test("reset restores every mutable switch and fixture dates", async () => {
    const handle = createMockHandler();
    const before = await (await handle(request("/debug/state"))).json();
    const window = await (await handle(request("/api/enrollment/window"))).json();
    for (const path of [
      "/debug/credentials/invalid",
      "/debug/reauth-mode/reject",
      "/debug/campus-phase/live",
      "/debug/refresh-mode/reject",
      "/debug/session/expire",
    ]) {
      expect((await handle(request(path, "POST"))).ok).toBe(true);
    }
    await handle(request("/debug/reset", "POST"));
    expect(await (await handle(request("/debug/state"))).json()).toEqual(before);
    expect(await (await handle(request("/api/enrollment/window"))).json()).toEqual(window);
    expect(await (await createMockHandler()(request("/api/enrollment/window"))).json()).toEqual(
      window,
    );
  });

  test("separate handlers never inherit state", async () => {
    const first = createMockHandler();
    const second = createMockHandler();
    await first(request("/debug/scenario/home.offline-with-cache", "POST"));
    expect((await first(request("/api/sync/profile"))).status).toBe(503);
    expect((await second(request("/api/sync/profile"))).status).toBe(200);
    expect((await second(request("/debug/scenario/typo", "POST"))).status).toBe(400);
  });

  test("expiration applies to credential status too and rotation recovers", async () => {
    const handle = createMockHandler();
    await handle(request("/debug/session/expire", "POST"));
    expect((await handle(request("/api/me/status"))).status).toBe(401);
    expect((await handle(request("/api/sync/profile"))).status).toBe(401);
    const refreshed = await handle(
      request("/api/auth/token/refresh", "POST", {
        accessToken: pilot.login.accessToken,
        refreshToken: pilot.login.refreshToken,
      }),
    );
    expect(await refreshed.json()).toMatchObject({
      data: {
        accessToken: "synthetic-access-1",
        refreshToken: "synthetic-refresh-1",
      },
    });
    expect((await handle(request("/api/me/status"))).status).toBe(200);
  });

  test("rejects spent refresh tokens and exposes terminal session failure", async () => {
    const handle = createMockHandler();
    const refresh = () =>
      handle(
        request("/api/auth/token/refresh", "POST", {
          accessToken: pilot.login.accessToken,
          refreshToken: pilot.login.refreshToken,
        }),
      );
    expect((await refresh()).status).toBe(200);
    expect((await refresh()).status).toBe(400);
    await handle(request("/debug/scenario/auth.session-expired", "POST"));
    expect((await refresh()).status).toBe(400);
  });

  test("malformed clock fails before serving", () => {
    expect(() => createMockHandler({ now: "not-a-date" })).toThrow("Invalid mock clock");
    expect(() => createMockHandler({ now: "2026-10-02T10:00:00" })).toThrow("Invalid mock clock");
  });

  test("unknown routes cannot hide behind outage injection or scenario switching", async () => {
    const handle = createMockHandler();
    await handle(request("/debug/scenario/home.offline-with-cache", "POST"));
    expect((await handle(request("/api/unknown"))).status).toBe(501);
    await handle(request("/debug/scenario/home.populated", "POST"));
    expect(await (await handle(request("/debug/state"))).json()).toMatchObject({
      data: {
        unexpectedRequests: ["GET /api/unknown"],
      },
    });
  });

  test("a rejected spent token cannot invalidate the next valid refresh", async () => {
    const handle = createMockHandler();
    const refresh = (version: number) =>
      handle(
        request("/api/auth/token/refresh", "POST", {
          accessToken: `synthetic-access-${version}`,
          refreshToken: `synthetic-refresh-${version}`,
        }),
      );
    expect((await refresh(0)).status).toBe(200);
    expect((await refresh(0)).status).toBe(400);
    expect((await refresh(1)).status).toBe(200);
  });
});
