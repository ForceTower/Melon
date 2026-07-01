import Foundation

enum APIError: Error, Equatable {
    case invalidResponse
    case badStatus(Int)
}
