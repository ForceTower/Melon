import Foundation
import Testing

@testable import UNESKit

struct EnrollmentMappingTests {
    @Test
    func windowDecodesTheSecondlessOffsetDates() throws {
        let json = """
        {
          "available": true,
          "window": {
            "semester": "2026.2",
            "state": "OPEN",
            "startDate": "2026-06-15T00:00-03:00",
            "endDate": "2026-06-22T23:59:00-03:00",
            "minHours": 240,
            "maxHours": 420,
            "useQueue": true,
            "courseId": 42
          }
        }
        """
        let dto = try JSONDecoder().decode(EnrollmentWindowResponseDTO.self, from: Data(json.utf8))
        let window = try #require(dto.domain)

        #expect(window.semester == "2026.2")
        #expect(window.state == .open)
        #expect(window.useQueue)

        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: -3 * 3600)!
        let start = calendar.dateComponents([.year, .month, .day, .hour, .minute], from: try #require(window.startDate))
        #expect((start.year, start.month, start.day, start.hour, start.minute) == (2026, 6, 15, 0, 0))
        let end = calendar.dateComponents([.hour, .minute], from: try #require(window.endDate))
        #expect((end.hour, end.minute) == (23, 59))
    }

    @Test
    func unavailableWindowMapsToNil() throws {
        let json = #"{ "available": false, "window": null }"#
        let dto = try JSONDecoder().decode(EnrollmentWindowResponseDTO.self, from: Data(json.utf8))
        #expect(dto.domain == nil)
    }

    @Test
    func offersMapTimesShiftsAndStableTints() throws {
        let json = """
        {
          "disciplines": [
            {
              "id": 202, "code": "TEC499", "name": "Sistemas Digitais",
              "workload": 60, "mandatory": true, "gradePeriod": 4, "suggestion": true,
              "prereqs": [],
              "sections": [
                {
                  "id": 30201, "label": "T01P01", "coursePreferential": true, "suggestion": true,
                  "vacancies": 40, "proposalsCount": 22, "allowsOtherDefault": true,
                  "waitlistCount": 0, "selected": false,
                  "meetings": [
                    {
                      "kind": "Teórica", "shift": "AFTERNOON", "professors": ["Roberto Sales"],
                      "room": "PAT54",
                      "slots": [{ "day": 1, "start": "13:30:00", "end": "15:30:00" }]
                    },
                    {
                      "kind": "Prática", "shift": "A definir", "professors": [],
                      "room": null,
                      "slots": []
                    }
                  ]
                }
              ]
            },
            {
              "id": 201, "code": "EXA427", "name": "Estruturas de Dados",
              "workload": 60, "mandatory": true, "gradePeriod": 4, "suggestion": false,
              "prereqs": [{ "code": "EXA418", "name": "Algoritmos II", "met": true }],
              "sections": []
            }
          ]
        }
        """
        let dto = try JSONDecoder().decode(EnrollmentOffersResponseDTO.self, from: Data(json.utf8))
        let disciplines = dto.domain

        // Tints follow code order regardless of catalogue order.
        #expect(disciplines.map(\.code) == ["TEC499", "EXA427"])
        #expect(disciplines.map(\.colorIndex) == [1, 0])

        let section = disciplines[0].sections[0]
        // "HH:mm:ss" clock times land as minutes since midnight.
        #expect(section.slots == [EnrollmentSlot(day: 1, startMinute: 13 * 60 + 30, endMinute: 15 * 60 + 30)])
        #expect(section.meetings[0].shift == .afternoon)
        // Unknown shift strings degrade to undefined, never fail the decode.
        #expect(section.meetings[1].shift == .undefined)
        #expect(disciplines[1].prereqs == [EnrollmentPrerequisite(code: "EXA418", name: "Algoritmos II", met: true)])
    }

    @Test
    func submitRequestKeepsTheWireKeys() throws {
        let request = EnrollmentSubmitRequestDTO([
            EnrollmentSelection(sectionId: 30101, allowsOther: true, waitlist: false)
        ])
        let data = try JSONEncoder().encode(request)
        let object = try #require(try JSONSerialization.jsonObject(with: data) as? [String: [[String: Any]]])
        let selection = try #require(object["selections"]?.first)

        #expect(selection["sectionId"] as? Int64 == 30101)
        #expect(selection["allowsOther"] as? Bool == true)
        #expect(selection["waitlist"] as? Bool == false)
    }
}
