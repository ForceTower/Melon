import XCTest

@MainActor
final class ScenarioJourneyTests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    override func tearDown() async throws {
        let (data, _) = try await URLSession.shared.data(from: URL(string: "http://127.0.0.1:8788/debug/state")!)
        let state = try JSONDecoder().decode(MockState.self, from: data)
        XCTAssertEqual(state.data.mode, "hermetic")
        XCTAssertTrue(state.data.unexpectedRequests.isEmpty, "Unmocked requests: \(state.data.unexpectedRequests)")
    }

    func testLoginSyncAndHome() async throws {
        try await selectScenario("home.populated")
        let app = launch()
        let username = app.textFields["auth.username"]
        XCTAssertTrue(username.waitForExistence(timeout: 10))
        XCTAssertFalse(app.buttons["auth.submit"].isEnabled)
        capture(app, scenario: "auth.login")

        username.tap()
        username.typeText("scenario")
        let password = app.secureTextFields["auth.password"]
        password.tap()
        password.typeText("synthetic-only\n")

        let enter = app.buttons["sync.enter"]
        XCTAssertTrue(enter.waitForExistence(timeout: 30), "Initial sync must reach Ready")
        capture(app, scenario: "sync.ready")
        enter.tap()
        XCTAssertTrue(app.tabBars.buttons["Hoje"].waitForExistence(timeout: 15))
        XCTAssertTrue(app.staticTexts["Algoritmos de Exemplo"].firstMatch.waitForExistence(timeout: 15))
        capture(app, scenario: "home.populated")

        try await selectScenario("messages.empty")
        app.tabBars.buttons["Mensagens"].tap()
        XCTAssertTrue(app.staticTexts["messages.empty"].waitForExistence(timeout: 15))
        try await waitForRequest("GET /api/sync/messages")
        capture(app, scenario: "messages.empty")

        try await selectScenario("home.offline-with-cache")
        app.tabBars.buttons["Hoje"].tap()
        app.scrollViews.firstMatch.swipeDown()
        try await waitForRequest("GET /api/sync/semesters")
        XCTAssertTrue(app.staticTexts["Algoritmos de Exemplo"].firstMatch.exists)
        capture(app, scenario: "home.offline-with-cache")
    }

    func testInvalidCredentialsStayOnLogin() async throws {
        try await selectScenario("auth.invalid-credentials")
        let app = launch()
        let username = app.textFields["auth.username"]
        XCTAssertTrue(username.waitForExistence(timeout: 10))
        username.tap()
        username.typeText("scenario")
        let password = app.secureTextFields["auth.password"]
        password.tap()
        password.typeText("wrong\n")
        XCTAssertTrue(app.staticTexts["auth.error"].waitForExistence(timeout: 10))
        XCTAssertEqual(app.staticTexts["auth.error"].label, "Usuário ou senha incorretos. Confira suas credenciais do SAGRES.")
        try await waitForRequest("POST /api/auth/login")
        XCTAssertFalse(app.buttons["sync.enter"].exists)
        capture(app, scenario: "auth.invalid-credentials")
    }

    func testLoginAccessibility() async throws {
        try await selectScenario("auth.login")
        let app = launch()
        XCTAssertTrue(app.textFields["auth.username"].waitForExistence(timeout: 10))
        try app.performAccessibilityAudit(for: [.elementDetection, .trait])
        capture(app, scenario: "auth.login.accessibility")
    }

    private func launch() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchEnvironment["MELON_SCENARIO"] = "auth.login"
        app.launchEnvironment["TZ"] = "America/Bahia"
        app.launchArguments = ["-AppleLanguages", "(pt-BR)", "-AppleLocale", "pt_BR"]
        app.launch()
        return app
    }

    private func selectScenario(_ id: String) async throws {
        var request = URLRequest(url: URL(string: "http://127.0.0.1:8788/debug/scenario/\(id)")!)
        request.httpMethod = "POST"
        let (_, response) = try await URLSession.shared.data(for: request)
        XCTAssertEqual((response as? HTTPURLResponse)?.statusCode, 200)
    }

    private func waitForRequest(_ key: String) async throws {
        for _ in 0..<30 {
            let (data, _) = try await URLSession.shared.data(from: URL(string: "http://127.0.0.1:8788/debug/state")!)
            let state = try JSONDecoder().decode(MockState.self, from: data)
            if (state.data.requestCounts[key] ?? 0) > 0 { return }
            try await Task.sleep(for: .milliseconds(250))
        }
        XCTFail("Expected refresh request did not reach the scenario server: \(key)")
    }

    private func capture(_ app: XCUIApplication, scenario: String) {
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = scenario
        screenshot.lifetime = .keepAlways
        add(screenshot)
        let hierarchy = XCTAttachment(string: app.debugDescription)
        hierarchy.name = "\(scenario).hierarchy"
        hierarchy.lifetime = .keepAlways
        add(hierarchy)
    }
}

private struct MockState: Decodable {
    let data: State

    struct State: Decodable {
        let mode: String
        let unexpectedRequests: [String]
        let requestCounts: [String: Int]
    }
}
