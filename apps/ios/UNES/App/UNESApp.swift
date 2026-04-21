//
//  UNESApp.swift
//  UNES
//
//  Created by João Paulo Santos Sena on 17/04/26.
//

import SwiftUI
import Umbrella

@main
struct UNESApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            let graph = appDelegate.graph
            RootView(
                sessionStore: graph.sessionStore,
                onboarding: graph.onboardingFactory,
                overview: graph.overviewFactory,
                scheduleFocused: graph.scheduleFocusedFactory
            )
        }
    }
}
