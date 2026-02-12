import XCTest
@testable import DokusShareExtension

final class ShareImportFailureLocalizationTests: XCTestCase {
    private lazy var rootBundle = Bundle(for: ShareImportFailureLocalizationTests.self)

    func test_localFailuresUseLocalizedKeysNotHardcodedMessages() {
        guard let localeBundle = ShareLocalization.bundle(for: "en", from: rootBundle) else {
            return XCTFail("Missing locale bundle: en")
        }

        let failure = ShareImportFailure(
            type: .payloadUnavailable,
            message: ShareLocalizedMessage(key: .errorNoPdfFound),
            retryable: false
        )

        let localized = failure.localizedMessage(
            bundle: localeBundle,
            locale: Locale(identifier: "en")
        )

        XCTAssertFalse(localized.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        XCTAssertNotEqual(localized, ShareLocalizationKey.errorNoPdfFound.rawValue)
        XCTAssertNil(failure.message.serverMessageOverride)
    }

    func test_serverMessageOverrideHasPriorityOverFallbackLocalization() {
        guard let localeBundle = ShareLocalization.bundle(for: "en", from: rootBundle) else {
            return XCTFail("Missing locale bundle: en")
        }

        let failure = ShareImportFailure(
            type: .upload,
            message: ShareLocalizedMessage(
                key: .errorUploadFailed,
                serverMessageOverride: "Backend rejected upload"
            ),
            retryable: true
        )

        let localized = failure.localizedMessage(
            bundle: localeBundle,
            locale: Locale(identifier: "en")
        )

        XCTAssertEqual(localized, "Backend rejected upload")
    }
}
