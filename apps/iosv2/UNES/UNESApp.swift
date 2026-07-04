//
//  UNESApp.swift
//  UNES
//
//  Created by João Paulo Santos Sena on 01/07/26.
//

import CoreSpotlight
import SwiftUI
import UNESKit

@main
struct UNESApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            RootView()
                .onContinueUserActivity(CSSearchableItemActionType) { activity in
                    guard let identifier =
                        activity.userInfo?[CSSearchableItemActivityIdentifier] as? String
                    else { return }
                    IntentSupport.openEntity(identifier: identifier)
                }
        }
    }
}
