import XCTest
@testable import Retrieval

final class FTSQueryTests: XCTestCase {
    func testStripsOperatorChars() {
        let q = ftsQuery(from: #"who is alice? "the lady""#)
        XCTAssertFalse(q.contains("?"))
        XCTAssertFalse(q.contains("\""))
        XCTAssertTrue(q.contains("alice"))
    }

    func testEmptyForShortInput() {
        XCTAssertEqual(ftsQuery(from: "a"), "")
        XCTAssertEqual(ftsQuery(from: ""), "")
    }

    func testJoinsWithOR() {
        let q = ftsQuery(from: "alice rabbit")
        XCTAssertTrue(q.contains("OR"))
    }
}
