//
//  UNESWatchApp.swift
//  UNESWatch
//
//  Created by João Paulo Santos Sena on 04/07/26.
//

import FirebaseCore
import FirebaseCrashlytics
import SwiftUI
import UNESKit

@main
struct UNESWatchApp: App {
    init() {
        // Xcode previews can launch app entry points too — configuring
        // Firebase there bugs Xcode out, so all of it is skipped (same
        // guard as the iPhone AppDelegate).
        let environment = ProcessInfo.processInfo.environment
        guard environment["XCODE_RUNNING_FOR_PREVIEWS"] != "1",
              environment["XCODE_RUNNING_FOR_PLAYGROUNDS"] != "1"
        else { return }
        FirebaseApp.configure()
        #if DEBUG
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(false)
        #endif
    }

    var body: some Scene {
        WindowGroup {
            WatchRootView()
        }
    }
}
