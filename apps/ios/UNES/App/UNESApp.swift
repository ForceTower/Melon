//
//  UNESApp.swift
//  UNES
//
//  Created by João Paulo Santos Sena on 17/04/26.
//

import SwiftUI

@main
struct UNESApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(\.umbrella, appDelegate.graph)
        }
    }
}
