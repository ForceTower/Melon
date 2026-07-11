import ComposableArchitecture
import Foundation

private let log = Log.scoped("CampusEventRepository")

extension CampusEventRepository: DependencyKey {
    static let liveValue = CampusEventRepository(
        cached: {
            @Dependency(\.database) var wrappedDatabase
            return try await MirrorStore(writer: wrappedDatabase).cachedCampusEvent()
        },
        refresh: {
            @Dependency(\.apiClient) var wrappedClient
            @Dependency(\.database) var wrappedDatabase
            @Dependency(\.date) var wrappedDate
            let apiClient = wrappedClient
            let mirror = MirrorStore(writer: wrappedDatabase)

            log.debug("refresh start")
            do {
                let dto: CampusEventEnvelopeDTO = try await apiClient.get(from: "api/campus-events/current")
                let event = dto.event?.domain
                try await mirror.applyCampusEvent(event, syncedAt: wrappedDate.now)
                if let event {
                    log.info("refresh ok id=\(event.id) revision=\(event.revision) activities=\(event.activities.count)")
                } else {
                    log.info("refresh ok — nothing featured")
                }
            } catch {
                logFailure("refresh", error: error)
                throw error
            }
        },
        observe: {
            @Dependency(\.database) var wrappedDatabase
            let mirror = MirrorStore(writer: wrappedDatabase)
            log.debug("observe subscribed")
            return AsyncStream { continuation in
                let task = Task {
                    // Observation only fails if the database itself is gone;
                    // ending the stream is all there is to do.
                    do {
                        for try await event in mirror.campusEventUpdates() {
                            continuation.yield(event)
                        }
                    } catch {
                        log.error("observe failed", error: error)
                    }
                    continuation.finish()
                }
                continuation.onTermination = { _ in task.cancel() }
            }
        }
    )

    private static func logFailure(_ operation: String, error: Error) {
        switch error {
        case APIError.server(401, _):
            log.warn("\(operation) unauthorized")
        case let APIError.server(status, message):
            log.warn("\(operation) server \(status) message=\(message ?? "<none>")")
        case APIError.emptyEnvelope:
            log.warn("\(operation) 2xx envelope had null data")
        case is URLError:
            log.warn("\(operation) transport failure", error: error)
        default:
            log.error("\(operation) failed", error: error)
        }
    }
}

// MARK: - DTOs (`api/campus-events/current`)

/// `event` is null when no event is featured for the student right now.
private struct CampusEventEnvelopeDTO: Decodable {
    var event: CampusEventDTO?
}

private struct CampusEventDTO: Decodable {
    struct Activity: Decodable {
        var id: String
        var title: String
        var details: String? = nil
        var category: String
        var audience: String? = nil
        var venueId: String? = nil
        var venueName: String
        var speakerIds: [String]? = nil
        var speakerNames: [String]? = nil
        var startsAt: Date
        var endsAt: Date? = nil
        var requiresSignup: Bool? = nil

        var domain: CampusEventActivity {
            CampusEventActivity(
                id: id, title: title, details: details,
                // Unknown kinds are newer server categories — generic, not dropped.
                category: CampusEventCategory(rawValue: category) ?? .other,
                audience: audience.flatMap(CampusEventAudience.init(rawValue:)) ?? .everyone,
                venueId: venueId, venueName: venueName,
                speakerIds: speakerIds ?? [], speakerNames: speakerNames ?? [],
                startsAt: startsAt, endsAt: endsAt,
                requiresSignup: requiresSignup ?? false
            )
        }
    }

    struct Speaker: Decodable {
        var id: String
        var name: String
        var role: String? = nil
        var organization: String? = nil
        var bio: String? = nil
        var tag: String? = nil

        var domain: CampusEventSpeaker {
            CampusEventSpeaker(id: id, name: name, role: role, organization: organization, bio: bio, tag: tag)
        }
    }

    struct Workshop: Decodable {
        var id: String
        var title: String
        var details: String? = nil
        var audience: String? = nil
        var venueName: String? = nil
        var instructors: String? = nil
        var requiresSignup: Bool? = nil
        var slots: Int? = nil

        var domain: CampusEventWorkshop {
            CampusEventWorkshop(
                id: id, title: title, details: details,
                audience: audience.flatMap(CampusEventAudience.init(rawValue:)) ?? .everyone,
                venueName: venueName, instructors: instructors,
                requiresSignup: requiresSignup ?? false, slots: slots
            )
        }
    }

    struct Venue: Decodable {
        var id: String
        var name: String
        var shortName: String? = nil
        var hint: String? = nil
        var mapX: Double? = nil
        var mapY: Double? = nil

        var domain: CampusEventVenue {
            CampusEventVenue(id: id, name: name, shortName: shortName, hint: hint, mapX: mapX, mapY: mapY)
        }
    }

    struct Organization: Decodable {
        var id: String
        var name: String
        var fullName: String? = nil
        var tag: String? = nil
        var details: String? = nil

        var domain: CampusEventOrganization {
            CampusEventOrganization(id: id, name: name, fullName: fullName, tag: tag, details: details)
        }
    }

    var id: String
    var revision: Int? = nil
    var name: String
    var edition: String? = nil
    var tagline: String? = nil
    var institution: String? = nil
    var credit: String? = nil
    var timezone: String? = nil
    var startsAt: Date
    var endsAt: Date
    var activities: [Activity]
    var speakers: [Speaker]? = nil
    var workshops: [Workshop]? = nil
    var venues: [Venue]? = nil
    var organizations: [Organization]? = nil

    var domain: CampusEvent {
        CampusEvent(
            id: id, revision: revision ?? 1,
            name: name, edition: edition, tagline: tagline,
            institution: institution, credit: credit,
            timeZoneIdentifier: timezone,
            startsAt: startsAt, endsAt: endsAt,
            activities: activities.map(\.domain),
            speakers: (speakers ?? []).map(\.domain),
            workshops: (workshops ?? []).map(\.domain),
            venues: (venues ?? []).map(\.domain),
            organizations: (organizations ?? []).map(\.domain)
        )
    }
}
