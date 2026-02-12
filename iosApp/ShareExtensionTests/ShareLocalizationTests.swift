import XCTest
@testable import DokusShareExtension

final class ShareLocalizationTests: XCTestCase {
    private let localeIdentifiers = ShareLocalization.supportedLocaleIdentifiers
    private lazy var rootBundle = Bundle(for: ShareLocalizationTests.self)

    func test_allLocalizationKeysExistInAllShareExtensionLocales() {
        for localeIdentifier in localeIdentifiers {
            guard let localeBundle = ShareLocalization.bundle(for: localeIdentifier, from: rootBundle) else {
                XCTFail("Missing locale bundle: \(localeIdentifier)")
                continue
            }

            for key in ShareLocalizationKey.allCases {
                let value = key.localized(
                    bundle: localeBundle,
                    locale: locale(for: localeIdentifier)
                )

                XCTAssertFalse(value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                XCTAssertNotEqual(
                    value,
                    key.rawValue,
                    "Missing key \(key.rawValue) for locale \(localeIdentifier)"
                )
            }
        }
    }

    func test_localizedFormattedStringsRenderForRepresentativeArguments() {
        guard let localeBundle = ShareLocalization.bundle(for: "en", from: rootBundle) else {
            return XCTFail("Missing locale bundle: en")
        }

        let uploading = ShareLocalizationKey.headingUploading.localized(
            arguments: [.string("Acme")],
            bundle: localeBundle,
            locale: Locale(identifier: "en")
        )
        XCTAssertTrue(uploading.contains("Acme"))
        XCTAssertFalse(uploading.contains("%@"))

        let fileProgress = ShareLocalizationKey.fileProgress.localized(
            arguments: [.int(2), .int(5)],
            bundle: localeBundle,
            locale: Locale(identifier: "en")
        )
        XCTAssertTrue(fileProgress.contains("2"))
        XCTAssertTrue(fileProgress.contains("5"))
        XCTAssertFalse(fileProgress.contains("%d"))

        let uploadedMultiple = ShareLocalizationKey.uploadedMultiple.localized(
            arguments: [.int(3)],
            bundle: localeBundle,
            locale: Locale(identifier: "en")
        )
        XCTAssertTrue(uploadedMultiple.contains("3"))
        XCTAssertFalse(uploadedMultiple.contains("%d"))
    }

    func test_noLocalizationKeyFallsBackToRawKeyInSupportedLocales() {
        for localeIdentifier in localeIdentifiers {
            guard let localeBundle = ShareLocalization.bundle(for: localeIdentifier, from: rootBundle) else {
                XCTFail("Missing locale bundle: \(localeIdentifier)")
                continue
            }

            for key in ShareLocalizationKey.allCases {
                let resolved = ShareLocalizedMessage(key: key).resolve(
                    bundle: localeBundle,
                    locale: locale(for: localeIdentifier)
                )
                XCTAssertNotEqual(
                    resolved,
                    key.rawValue,
                    "Fallback to raw key for locale \(localeIdentifier), key \(key.rawValue)"
                )
            }
        }
    }

    private func locale(for localeIdentifier: String) -> Locale {
        if localeIdentifier == "Base" {
            return Locale(identifier: "en")
        }

        return Locale(identifier: localeIdentifier)
    }
}
