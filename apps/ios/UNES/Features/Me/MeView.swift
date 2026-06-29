import SwiftUI

/// "Eu" tab — personal hub that replaces the old menu list. Composed of:
/// identity hero, semester progress strip, pinned shortcut grid, services
/// list, sign-out pill, and a version footer. Mirrors `MeScreen` in
/// `screens-me.jsx`; the tweak-panel toggles live in Settings instead of an
/// in-screen overlay.
struct MeView: View {
    @State private var viewModel: MeViewModel
    // Shortcut grid + settings rows are UI affordances, not user data — they
    // keep reading from `MeFixtures`. Only the hero / semester strip / CR /
    // credits are viewmodel-driven.
    // Pinned shortcuts; "Matrícula" only appears while the enrollment window
    // is open (`MeViewModel.enrollmentAvailable`).
    private var pinned: [Shortcut] {
        var ids: [Shortcut.Kind] = []
        if viewModel.enrollmentAvailable { ids.append(.enrollment) }
        ids.append(contentsOf: [.calendar, .countdown])
        return MeFixtures.pinned(from: ids)
    }
    private let settingsFactory: SettingsFactory?
    private let calendarFactory: CalendarFactory?
    private let onLoggedOut: () -> Void
    // Initial guess used on first present; swapped for the measured height
    // once `LogoutConfirmationSheet` reports its intrinsic size so the
    // detent never leaves empty space below the buttons.
    @State private var logoutSheetHeight: CGFloat = 380
    // Same intrinsic-height pattern for the "Sobre o aplicativo" sheet.
    @State private var aboutSheetHeight: CGFloat = 540
    @State private var aboutPresented: Bool = false
    // The enrollment flow's view model — loads the live window + offers and owns
    // the in-progress proposal. Pushed onto the hub stack (value-routed via
    // `EnrollmentRoute`) so every step gets the native back chevron and the
    // entry screen returns here.
    @State private var enrollVM: EnrollmentViewModel
    // Type-erased path — the Me hub can push both `Shortcut.Kind` (from the
    // shortcut grid) and `MeSettingsRow.Kind` (from the settings list) onto
    // the same stack.
    @State private var path = NavigationPath()

    init(factory: MeFactory, onLoggedOut: @escaping () -> Void = {}) {
        _viewModel = State(initialValue: factory.makeViewModel())
        _enrollVM = State(initialValue: factory.enrollmentFactory.makeViewModel())
        self.settingsFactory = factory.settingsFactory
        self.calendarFactory = factory.calendarFactory
        self.onLoggedOut = onLoggedOut
    }

    // Factory-less init — retained so `#Preview` keeps rendering against
    // `MeFixtures` without a live graph.
    init() {
        _viewModel = State(initialValue: MeViewModel())
        _enrollVM = State(initialValue: EnrollmentViewModel())
        self.settingsFactory = nil
        self.calendarFactory = nil
        self.onLoggedOut = {}
    }

    // Identity strip for the enrollment entry screen, sourced from the signed-in
    // profile (the enrollment feed doesn't carry the student).
    private var enrollStudent: EnrollmentStudent {
        guard let identity = viewModel.identity else { return EnrollmentFixtures.student }
        return EnrollmentStudent(
            name: identity.name,
            course: identity.course,
            period: identity.semester,
            avatarInitial: identity.avatarInitial
        )
    }

    // Nil until the profile flow emits — `screenBody` hides the hero in that
    // window rather than substitute fake fixture content. `MeFixtures.identity`
    // is preview-only.
    private var identity: ProfileIdentity? {
        viewModel.identity
    }

    var body: some View {
        NavigationStack(path: $path) {
            screenBody
                .navigationTitle("Eu")
                .toolbar(.hidden, for: .navigationBar)
                .navigationDestination(for: Shortcut.Kind.self) { kind in
                    switch kind {
                    case .countdown:
                        FinalCountdownView()
                    case .calendar:
                        if let calendarFactory {
                            CalendarView(factory: calendarFactory)
                        } else {
                            CalendarView()
                        }
                    default:
                        EmptyView()
                    }
                }
                .navigationDestination(for: EnrollmentRoute.self) { route in
                    enrollmentDestination(for: route)
                        .toolbar(.hidden, for: .tabBar)
                        .task { await enrollVM.loadIfNeeded() }
                }
                .navigationDestination(for: MeSettingsRow.Kind.self) { kind in
                    switch kind {
                    case .settings:
                        if let settingsFactory {
                            SettingsView(factory: settingsFactory)
                        } else {
                            // Factory-less preview / fixture path.
                            SettingsView()
                        }
                    case .licenses:
                        // Reads the license-plist artifact bundled at build
                        // time — no factory wiring needed.
                        LicensesView()
                    default:
                        EmptyView()
                    }
                }
                .task { await viewModel.observe() }
                .sheet(isPresented: logoutSheetBinding) {
                    logoutSheet
                }
                .sheet(isPresented: $aboutPresented) {
                    aboutSheetBody
                }
                .overlay {
                    if viewModel.logoutStep == .flashing {
                        LogoutFlashView()
                            .transition(.opacity)
                    }
                }
                .fullScreenCover(isPresented: loggedOutCoverBinding) {
                    LoggedOutView(firstName: viewModel.logoutName) {
                        onLoggedOut()
                    }
                }
        }
    }

    private var logoutSheetBinding: Binding<Bool> {
        Binding(
            get: { viewModel.logoutStep == .confirming },
            set: { showing in
                if !showing, viewModel.logoutStep == .confirming {
                    viewModel.cancelLogout()
                }
            }
        )
    }

    private var loggedOutCoverBinding: Binding<Bool> {
        Binding(
            get: { viewModel.logoutStep == .loggedOut },
            set: { _ in }
        )
    }

    @ViewBuilder
    private var aboutSheetBody: some View {
        if #available(iOS 16.4, *) {
            AboutSheet(info: AppInfo.current, measuredHeight: $aboutSheetHeight)
                .presentationDetents([.height(aboutSheetHeight)])
                .presentationDragIndicator(.visible)
                .presentationBackground(UNESColor.surface)
        } else {
            AboutSheet(info: AppInfo.current, measuredHeight: $aboutSheetHeight)
                .presentationDetents([.height(aboutSheetHeight)])
                .presentationDragIndicator(.visible)
        }
    }

    @ViewBuilder
    private var logoutSheet: some View {
        if let identity {
            if #available(iOS 16.4, *) {
                LogoutConfirmationSheet(
                    identity: identity,
                    onCancel: { viewModel.cancelLogout() },
                    onConfirm: { keepData in
                        Task { await viewModel.confirmLogout(keepData: keepData) }
                    },
                    measuredHeight: $logoutSheetHeight
                )
                .presentationDetents([.height(logoutSheetHeight)])
                .presentationDragIndicator(.visible)
                .presentationBackground(UNESColor.surface)
            } else {
                LogoutConfirmationSheet(
                    identity: identity,
                    onCancel: { viewModel.cancelLogout() },
                    onConfirm: { keepData in
                        Task { await viewModel.confirmLogout(keepData: keepData) }
                    },
                    measuredHeight: $logoutSheetHeight
                )
                .presentationDetents([.height(logoutSheetHeight)])
                .presentationDragIndicator(.visible)
            }
        }
    }

    private var screenBody: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()

            // Ambient rose mesh pinned behind the hero, fading into the
            // surface so it never reads as a hard seam. Intensity folds in
            // what the HTML prototype achieves with a wrapper `opacity: 0.4`
            // — applying opacity to the whole container separately from the
            // gradient produces a visible cream-on-cream step at the frame
            // edge, so we bake the dimness into the mesh itself instead.
            VStack(spacing: 0) {
                ZStack {
                    MeshGradientView(variant: .rose, intensity: 0.22)
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0),
                            .init(color: UNESColor.surface, location: 1.0),
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
                .frame(height: 320)
                Spacer(minLength: 0)
            }
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    if let identity {
                        MeHeader(identity: identity)
                            .fadeUpOnAppear(delay: 0.02, distance: 12, duration: 0.55)
                    }

                    VStack(spacing: 14) {
                        if let identity {
                            IdentityCard(identity: identity)
                                .fadeScaleInOnAppear(delay: 0.12, from: 0.985, duration: 0.6, anchor: .top)

                            SemesterStrip(identity: identity)
                                .fadeUpOnAppear(delay: 0.22, distance: 12, duration: 0.55)
                        }

                        VStack(spacing: 0) {
                            MeSectionLabel(label: "atalhos fixados")
                            ShortcutGrid(shortcuts: pinned, onTap: handleShortcutTap)
                        }
                        .fadeUpOnAppear(delay: 0.3, distance: 12, duration: 0.55)

                        VStack(spacing: 0) {
                            MeSectionLabel(label: "definições")
                            SettingsCard(
                                rows: MeFixtures.settingsRows(syncHint: viewModel.lastSyncHint),
                                onTap: handleSettingsRowTap
                            )
                        }
                        .fadeUpOnAppear(delay: 0.38, distance: 12, duration: 0.55)

                        SignOutButton(action: { viewModel.beginLogout() })
                            .fadeUpOnAppear(delay: 0.46, distance: 12, duration: 0.55)

                        footer
                            .fadeUpOnAppear(delay: 0.54, distance: 12, duration: 0.55)
                    }
                    .padding(.horizontal, 14)
                    .padding(.bottom, 24)
                }
            }
        }
    }

    private func handleShortcutTap(_ kind: Shortcut.Kind) {
        switch kind {
        case .countdown, .calendar:
            path.append(kind)
        case .enrollment:
            guard viewModel.enrollmentAvailable else { return }
            path.append(EnrollmentRoute.window)
        default:
            break
        }
    }

    // Builds each screen of the matrícula flow for this hub's stack, mirroring
    // `OnboardingFlow.destination(for:)`. The entry screen owns the load / empty
    // states so the inner screens always receive a resolved `window`.
    @ViewBuilder
    private func enrollmentDestination(for route: EnrollmentRoute) -> some View {
        switch route {
        case .window:
            if let window = enrollVM.window {
                WindowStatusView(
                    enroll: enrollVM.enroll, window: window, windowState: enrollVM.windowState, student: enrollStudent,
                    onStart: { path.append(EnrollmentRoute.offers) },
                    onReview: { path.append(EnrollmentRoute.review) }
                )
            } else if let error = enrollVM.loadError {
                EnrollmentLoadFailedView(message: error) { await enrollVM.retry() }
            } else {
                EnrollmentLoadingView()
            }
        case .offers:
            enrollmentScreen { window in
                if enrollVM.offersLoading {
                    EnrollmentLoadingView()
                } else {
                    OffersView(
                        enroll: enrollVM.enroll, window: window, disciplines: enrollVM.disciplines,
                        onOpenDiscipline: { path.append(EnrollmentRoute.picker($0.id)) },
                        onTimetable: { path.append(EnrollmentRoute.timetable) },
                        onReview: { path.append(EnrollmentRoute.review) }
                    )
                }
            }
        case .picker(let id):
            enrollmentScreen { window in
                if let discipline = enrollVM.disciplines.first(where: { $0.id == id }) {
                    SectionPickerView(
                        discipline: discipline, enroll: enrollVM.enroll, window: window,
                        onTimetable: { path.append(EnrollmentRoute.timetable) }
                    )
                }
            }
        case .timetable:
            enrollmentScreen { window in
                EnrollmentTimetableView(
                    enroll: enrollVM.enroll, window: window,
                    onReview: { path.append(EnrollmentRoute.review) }
                )
            }
        case .review:
            enrollmentScreen { window in
                ReviewView(
                    enroll: enrollVM.enroll, window: window, windowState: enrollVM.windowState,
                    onTimetable: { path.append(EnrollmentRoute.timetable) },
                    onSubmit: {
                        let error = await enrollVM.submit()
                        if error == nil { path.append(EnrollmentRoute.success) }
                        return error
                    }
                )
            }
        case .success:
            enrollmentScreen { window in
                SuccessView(
                    enroll: enrollVM.enroll, window: window,
                    onDone: { path = NavigationPath() }
                )
            }
        }
    }

    // Inner matrícula screens need a resolved window; show the loader until the
    // entry screen's fetch lands it.
    @ViewBuilder
    private func enrollmentScreen(@ViewBuilder _ content: (EnrollmentWindow) -> some View) -> some View {
        if let window = enrollVM.window {
            content(window)
        } else {
            EnrollmentLoadingView()
        }
    }

    private func handleSettingsRowTap(_ row: MeSettingsRow) {
        switch row.id {
        case .settings, .licenses:
            path.append(row.id)
        case .about:
            aboutPresented = true
        case .feedback:
            sendFeedbackSMS()
        default:
            break
        }
    }

    private func sendFeedbackSMS() {
        let app = AppInfo.current
        let info = """
        UNES \(app.version)(\(app.build)) - \(AppInfo.modelIdentifier()) \(Locale.current.identifier)
        id: \(app.machineId)
        Os dados acima me ajudam a encontrar o erro, não apaga se puder :)
        """.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? ""
        guard let url = URL(string: "sms:joaopaulo761@gmail.com?&body=\(info)") else { return }
        if UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url)
        }
    }

    private var footer: some View {
        Text("◦ UNES V\(Bundle.main.appVersion) · FEITO COM ♥ EM FEIRA DE SANTANA ◦")
            .font(UNESFont.mono(9))
            .tracking(1.26)
            .foregroundStyle(UNESColor.ink4)
            .frame(maxWidth: .infinity)
            .padding(.top, 4)
            .padding(.bottom, 8)
    }
}

#Preview {
    MeView()
}
