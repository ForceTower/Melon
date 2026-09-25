import CoreGraphics
import SwiftUI
import Testing

@testable import UNESKit

/// Fixtures are scene readings from the iPhone Duo simulator (iOS 27.1 beta 1)
/// and the usual suspects beside it.
struct SpreadLayoutTests {
    @Test
    func foldedDuoAndPhonesStack() {
        #expect(PageLayout.resolve(width: 466, sizeClass: .compact) == .stack)
        #expect(PageLayout.resolve(width: 402, sizeClass: .compact) == .stack)
    }

    @Test
    func openDuoAndPadsSpread() {
        #expect(PageLayout.resolve(width: 951, sizeClass: .regular) == .spread)
        #expect(PageLayout.resolve(width: 744, sizeClass: .regular) == .spread)
        #expect(PageLayout.resolve(width: 1366, sizeClass: .regular) == .spread)
    }

    @Test
    func openDuoHeldUprightIsRegularButTooNarrowToSpread() {
        #expect(PageLayout.resolve(width: 669, sizeClass: .regular) == .stack)
    }

    @Test
    func slimPadSplitStacks() {
        #expect(PageLayout.resolve(width: 320, sizeClass: .compact) == .stack)
    }

    @Test
    func openDuoReportsASpineWhetherOrNotItIsBent() {
        let frame = CGRect(x: 455, y: 0, width: 40, height: 669)
        let size = CGSize(width: 951, height: 669)

        let flat = DeviceFold.make(frame: frame, isBent: false, in: size)
        #expect(flat?.axis == .spine)
        #expect(flat?.spineX == CGFloat(475))
        #expect(flat?.isBook == false)

        let bent = DeviceFold.make(frame: frame, isBent: true, in: size)
        #expect(bent?.spineX == CGFloat(475))
        #expect(bent?.isBook == true)
    }

    @Test
    func uprightDuoReportsAWaistWithNoSpine() {
        let frame = CGRect(x: 0, y: 455, width: 669, height: 40)
        let fold = DeviceFold.make(frame: frame, isBent: true, in: CGSize(width: 669, height: 951))
        #expect(fold?.axis == .waist)
        #expect(fold?.spineX == nil)
        #expect(fold?.isBook == false)
    }

    @Test
    func aFoldAtTheEdgeOfASplitViewHalfIsIgnored() {
        let frame = CGRect(x: 435, y: 0, width: 40, height: 669)
        #expect(DeviceFold.make(frame: frame, isBent: true, in: CGSize(width: 475, height: 669)) == nil)
    }

    @Test
    func aPartialRegionIsNotAFold() {
        let frame = CGRect(x: 455, y: 0, width: 40, height: 200)
        #expect(DeviceFold.make(frame: frame, isBent: true, in: CGSize(width: 951, height: 669)) == nil)
    }
}
