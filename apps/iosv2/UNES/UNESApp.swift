//
//  UNESApp.swift
//  UNES
//
//  Created by João Paulo Santos Sena on 01/07/26.
//

import SwiftUI
import UNESKit

@main
struct UNESApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
