import Foundation
import FileProvider
import UniformTypeIdentifiers

enum DokusPlacement: Hashable {
    case inbox
    case needsReview
    case typed(DokusTypedFolder)
}

struct DokusProjectedItem: Hashable {
    let identifier: NSFileProviderItemIdentifier
    let parentIdentifier: NSFileProviderItemIdentifier
    let filename: String
    let contentType: UTType
    let isFolder: Bool
    let capabilities: NSFileProviderItemCapabilities
    let documentSize: Int64?
    let creationDate: Date?
    let contentModificationDate: Date?
    let childItemCount: Int?
    let workspaceId: String?
    let documentId: String?
    let placement: DokusPlacement?
}

struct DokusProjection {
    var itemsByIdentifier: [NSFileProviderItemIdentifier: DokusProjectedItem]
    var childrenByParent: [NSFileProviderItemIdentifier: [NSFileProviderItemIdentifier]]
    var generation: Int64

    func item(_ identifier: NSFileProviderItemIdentifier) -> DokusProjectedItem? {
        itemsByIdentifier[identifier]
    }

    func children(of identifier: NSFileProviderItemIdentifier) -> [DokusProjectedItem] {
        let ids = childrenByParent[identifier] ?? []
        return ids.compactMap { itemsByIdentifier[$0] }
    }
}

enum DokusIdentifierKind: Hashable {
    case root
    case workspace(workspaceId: String)
    case lifecycleFolder(workspaceId: String, folder: DokusLifecycleFolder)
    case typedFolder(workspaceId: String, folder: DokusTypedFolder)
    case yearFolder(workspaceId: String, folder: DokusTypedFolder, year: Int)
    case monthFolder(workspaceId: String, folder: DokusTypedFolder, year: Int, month: Int)
    case document(workspaceId: String, documentId: String)
}

enum DokusItemIdentifierCodec {
    static func encode(kind: DokusIdentifierKind) -> NSFileProviderItemIdentifier {
        switch kind {
        case .root:
            return .rootContainer
        case .workspace(let workspaceId):
            return NSFileProviderItemIdentifier("dokus:ws:\(workspaceId)")
        case .lifecycleFolder(let workspaceId, let folder):
            return NSFileProviderItemIdentifier("dokus:lf:\(workspaceId):\(folder.rawValue)")
        case .typedFolder(let workspaceId, let folder):
            return NSFileProviderItemIdentifier("dokus:tf:\(workspaceId):\(folder.rawValue)")
        case .yearFolder(let workspaceId, let folder, let year):
            return NSFileProviderItemIdentifier("dokus:yr:\(workspaceId):\(folder.rawValue):\(year)")
        case .monthFolder(let workspaceId, let folder, let year, let month):
            return NSFileProviderItemIdentifier("dokus:mo:\(workspaceId):\(folder.rawValue):\(year):\(month)")
        case .document(let workspaceId, let documentId):
            return NSFileProviderItemIdentifier("dokus:doc:\(workspaceId):\(documentId)")
        }
    }

    static func decode(_ identifier: NSFileProviderItemIdentifier) -> DokusIdentifierKind? {
        if identifier == .rootContainer {
            return .root
        }

        let raw = identifier.rawValue
        let parts = raw.split(separator: ":", omittingEmptySubsequences: false).map(String.init)
        guard parts.count >= 3, parts[0] == "dokus" else {
            return nil
        }

        switch parts[1] {
        case "ws":
            guard parts.count == 3 else { return nil }
            return .workspace(workspaceId: parts[2])
        case "lf":
            guard parts.count == 4, let folder = DokusLifecycleFolder(rawValue: parts[3]) else { return nil }
            return .lifecycleFolder(workspaceId: parts[2], folder: folder)
        case "tf":
            guard parts.count == 4, let folder = DokusTypedFolder(rawValue: parts[3]) else { return nil }
            return .typedFolder(workspaceId: parts[2], folder: folder)
        case "yr":
            guard
                parts.count == 5,
                let folder = DokusTypedFolder(rawValue: parts[3]),
                let year = Int(parts[4])
            else { return nil }
            return .yearFolder(workspaceId: parts[2], folder: folder, year: year)
        case "mo":
            guard
                parts.count == 6,
                let folder = DokusTypedFolder(rawValue: parts[3]),
                let year = Int(parts[4]),
                let month = Int(parts[5])
            else { return nil }
            return .monthFolder(workspaceId: parts[2], folder: folder, year: year, month: month)
        case "doc":
            guard parts.count == 4 else { return nil }
            return .document(workspaceId: parts[2], documentId: parts[3])
        default:
            return nil
        }
    }
}

final class DokusProjectionBuilder {
    private let monthFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "MMMM"
        return formatter
    }()

    private let calendar = Calendar(identifier: .gregorian)
    private let filenameFormatter = DokusFilenameFormatter()

    func build(
        workspaces: [DokusWorkspace],
        documentsByWorkspace: [String: [DokusDocumentRecord]]
    ) -> DokusProjection {
        var itemsById: [NSFileProviderItemIdentifier: DokusProjectedItem] = [:]
        var childrenByParent: [NSFileProviderItemIdentifier: [NSFileProviderItemIdentifier]] = [:]

        addItem(
            DokusProjectedItem(
                identifier: .rootContainer,
                parentIdentifier: .rootContainer,
                filename: DokusFileProviderConstants.domainDisplayName,
                contentType: .folder,
                isFolder: true,
                capabilities: [.allowsReading, .allowsContentEnumerating],
                documentSize: nil,
                creationDate: nil,
                contentModificationDate: nil,
                childItemCount: nil,
                workspaceId: nil,
                documentId: nil,
                placement: nil
            ),
            to: &itemsById,
            children: &childrenByParent
        )

        itemsById[.workingSet] = DokusProjectedItem(
            identifier: .workingSet,
            parentIdentifier: .rootContainer,
            filename: "Working Set",
            contentType: .folder,
            isFolder: true,
            capabilities: [.allowsReading, .allowsContentEnumerating],
            documentSize: nil,
            creationDate: nil,
            contentModificationDate: nil,
            childItemCount: nil,
            workspaceId: nil,
            documentId: nil,
            placement: nil
        )

        let sortedWorkspaces = workspaces.sorted {
            $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending
        }

        for workspace in sortedWorkspaces {
            let workspaceId = workspace.id
            let workspaceIdentifier = DokusItemIdentifierCodec.encode(kind: .workspace(workspaceId: workspaceId))

            addItem(
                DokusProjectedItem(
                    identifier: workspaceIdentifier,
                    parentIdentifier: .rootContainer,
                    filename: workspace.name,
                    contentType: .folder,
                    isFolder: true,
                    capabilities: [.allowsReading, .allowsContentEnumerating],
                    documentSize: nil,
                    creationDate: nil,
                    contentModificationDate: nil,
                    childItemCount: nil,
                    workspaceId: workspaceId,
                    documentId: nil,
                    placement: nil
                ),
                to: &itemsById,
                children: &childrenByParent
            )

            let supportsLifecycle = workspace.role?.canSeeLifecycleFolders ?? true
            if supportsLifecycle {
                for lifecycle in DokusLifecycleFolder.allCases {
                    let lifecycleId = DokusItemIdentifierCodec.encode(
                        kind: .lifecycleFolder(workspaceId: workspaceId, folder: lifecycle)
                    )
                    let capabilities: NSFileProviderItemCapabilities = lifecycle == .inbox
                        ? [.allowsReading, .allowsContentEnumerating, .allowsAddingSubItems]
                        : [.allowsReading, .allowsContentEnumerating]

                    addItem(
                        DokusProjectedItem(
                            identifier: lifecycleId,
                            parentIdentifier: workspaceIdentifier,
                            filename: lifecycle.rawValue,
                            contentType: .folder,
                            isFolder: true,
                            capabilities: capabilities,
                            documentSize: nil,
                            creationDate: nil,
                            contentModificationDate: nil,
                            childItemCount: nil,
                            workspaceId: workspaceId,
                            documentId: nil,
                            placement: lifecycle == .inbox ? .inbox : .needsReview
                        ),
                        to: &itemsById,
                        children: &childrenByParent
                    )
                }
            }

            for typed in DokusTypedFolder.allCases {
                let typedId = DokusItemIdentifierCodec.encode(kind: .typedFolder(workspaceId: workspaceId, folder: typed))
                addItem(
                    DokusProjectedItem(
                        identifier: typedId,
                        parentIdentifier: workspaceIdentifier,
                        filename: typed.rawValue,
                        contentType: .folder,
                        isFolder: true,
                        capabilities: [.allowsReading, .allowsContentEnumerating],
                        documentSize: nil,
                        creationDate: nil,
                        contentModificationDate: nil,
                        childItemCount: nil,
                        workspaceId: workspaceId,
                        documentId: nil,
                        placement: .typed(typed)
                    ),
                    to: &itemsById,
                    children: &childrenByParent
                )
            }

            let records = documentsByWorkspace[workspaceId] ?? []
            let visibleRecords = projectRecords(
                records: records,
                workspace: workspace,
                itemsById: &itemsById,
                childrenByParent: &childrenByParent
            )

            for record in visibleRecords {
                addItem(record, to: &itemsById, children: &childrenByParent)
            }
        }

        return DokusProjection(
            itemsByIdentifier: itemsById,
            childrenByParent: childrenByParent,
            generation: 0
        )
    }

    private func projectRecords(
        records: [DokusDocumentRecord],
        workspace: DokusWorkspace,
        itemsById: inout [NSFileProviderItemIdentifier: DokusProjectedItem],
        childrenByParent: inout [NSFileProviderItemIdentifier: [NSFileProviderItemIdentifier]]
    ) -> [DokusProjectedItem] {
        var projected: [DokusProjectedItem] = []
        var duplicateIndex: [NSFileProviderItemIdentifier: [String: Int]] = [:]
        var createdYearFolders = Set<NSFileProviderItemIdentifier>()
        var createdMonthFolders = Set<NSFileProviderItemIdentifier>()

        let supportsLifecycle = workspace.role?.canSeeLifecycleFolders ?? true
        let workspaceId = workspace.id

        for record in records {
            guard let placement = resolvePlacement(for: record) else {
                continue
            }

            if !supportsLifecycle && (placement == .inbox || placement == .needsReview) {
                continue
            }

            let parentIdentifier: NSFileProviderItemIdentifier
            switch placement {
            case .inbox:
                parentIdentifier = DokusItemIdentifierCodec.encode(
                    kind: .lifecycleFolder(workspaceId: workspaceId, folder: .inbox)
                )
            case .needsReview:
                parentIdentifier = DokusItemIdentifierCodec.encode(
                    kind: .lifecycleFolder(workspaceId: workspaceId, folder: .needsReview)
                )
            case .typed(let typedFolder):
                if typedFolder == .exports {
                    continue
                }

                let issueDate = record.issueDate ?? record.uploadedAt ?? Date()
                let year = calendar.component(.year, from: issueDate)
                let month = calendar.component(.month, from: issueDate)

                let yearIdentifier = DokusItemIdentifierCodec.encode(
                    kind: .yearFolder(workspaceId: workspaceId, folder: typedFolder, year: year)
                )
                if !createdYearFolders.contains(yearIdentifier) {
                    createdYearFolders.insert(yearIdentifier)
                    let parentTyped = DokusItemIdentifierCodec.encode(
                        kind: .typedFolder(workspaceId: workspaceId, folder: typedFolder)
                    )
                    addItem(
                        DokusProjectedItem(
                            identifier: yearIdentifier,
                            parentIdentifier: parentTyped,
                            filename: "\(year)",
                            contentType: .folder,
                            isFolder: true,
                            capabilities: [.allowsReading, .allowsContentEnumerating],
                            documentSize: nil,
                            creationDate: nil,
                            contentModificationDate: nil,
                            childItemCount: nil,
                            workspaceId: workspaceId,
                            documentId: nil,
                            placement: .typed(typedFolder)
                        ),
                        to: &itemsById,
                        children: &childrenByParent
                    )
                }

                let monthIdentifier = DokusItemIdentifierCodec.encode(
                    kind: .monthFolder(workspaceId: workspaceId, folder: typedFolder, year: year, month: month)
                )
                if !createdMonthFolders.contains(monthIdentifier) {
                    createdMonthFolders.insert(monthIdentifier)
                    let monthName = monthFormatter.monthSymbols[month - 1]
                    let monthLabel = String(format: "%02d%@%@", month, DokusFileProviderConstants.monthSeparator, monthName)

                    addItem(
                        DokusProjectedItem(
                            identifier: monthIdentifier,
                            parentIdentifier: yearIdentifier,
                            filename: monthLabel,
                            contentType: .folder,
                            isFolder: true,
                            capabilities: [.allowsReading, .allowsContentEnumerating],
                            documentSize: nil,
                            creationDate: nil,
                            contentModificationDate: nil,
                            childItemCount: nil,
                            workspaceId: workspaceId,
                            documentId: nil,
                            placement: .typed(typedFolder)
                        ),
                        to: &itemsById,
                        children: &childrenByParent
                    )
                }

                parentIdentifier = monthIdentifier
            }

            guard itemsById[parentIdentifier] != nil else {
                continue
            }

            let fileIdentifier = DokusItemIdentifierCodec.encode(
                kind: .document(workspaceId: workspaceId, documentId: record.documentId)
            )
            let filenameBase = filenameFormatter.filename(for: record, placement: placement)
            let filename = deduplicateFilename(
                filenameBase,
                parentIdentifier: parentIdentifier,
                duplicateIndex: &duplicateIndex
            )

            let capabilities: NSFileProviderItemCapabilities = placement == .inbox
                ? [.allowsReading, .allowsDeleting]
                : [.allowsReading]

            projected.append(
                DokusProjectedItem(
                    identifier: fileIdentifier,
                    parentIdentifier: parentIdentifier,
                    filename: filename,
                    contentType: UTType.fromMimeType(record.contentType),
                    isFolder: false,
                    capabilities: capabilities,
                    documentSize: record.sizeBytes,
                    creationDate: record.issueDate ?? record.uploadedAt,
                    contentModificationDate: record.updatedAt ?? record.uploadedAt,
                    childItemCount: nil,
                    workspaceId: workspaceId,
                    documentId: record.documentId,
                    placement: placement
                )
            )
        }

        return projected
    }

    private func resolvePlacement(for record: DokusDocumentRecord) -> DokusPlacement? {
        if record.latestIngestionStatus == .queued || record.latestIngestionStatus == .processing {
            return .inbox
        }

        if record.draftStatus == .needsReview || record.latestIngestionStatus == .failed {
            return .needsReview
        }

        guard record.draftStatus == .confirmed else {
            return .needsReview
        }

        guard let type = record.draftType else {
            return .needsReview
        }

        switch type {
        case .invoice:
            switch record.draftDirection {
            case .inbound: return .typed(.invoicesIn)
            case .outbound: return .typed(.invoicesOut)
            case .unknown: return .needsReview
            }
        case .creditNote:
            switch record.draftDirection {
            case .inbound: return .typed(.creditNotesIn)
            case .outbound: return .typed(.creditNotesOut)
            case .unknown: return .needsReview
            }
        case .receipt:
            switch record.draftDirection {
            case .inbound: return .typed(.receiptsIn)
            case .outbound: return .typed(.receiptsOut)
            case .unknown: return .needsReview
            }
        case .quote:
            return .typed(.quotes)
        case .proForma:
            return .typed(.proForma)
        case .unknown:
            return .needsReview
        }
    }

    private func deduplicateFilename(
        _ candidate: String,
        parentIdentifier: NSFileProviderItemIdentifier,
        duplicateIndex: inout [NSFileProviderItemIdentifier: [String: Int]]
    ) -> String {
        var byName = duplicateIndex[parentIdentifier] ?? [:]
        let counter = (byName[candidate] ?? 0) + 1
        byName[candidate] = counter
        duplicateIndex[parentIdentifier] = byName

        guard counter > 1 else {
            return candidate
        }

        let baseURL = URL(fileURLWithPath: candidate)
        let extensionPart = baseURL.pathExtension
        let stem = baseURL.deletingPathExtension().lastPathComponent
        if extensionPart.isEmpty {
            return "\(stem) (\(counter))"
        }
        return "\(stem) (\(counter)).\(extensionPart)"
    }

    private func addItem(
        _ item: DokusProjectedItem,
        to itemsById: inout [NSFileProviderItemIdentifier: DokusProjectedItem],
        children: inout [NSFileProviderItemIdentifier: [NSFileProviderItemIdentifier]]
    ) {
        itemsById[item.identifier] = item
        if item.identifier == .rootContainer {
            return
        }
        children[item.parentIdentifier, default: []].append(item.identifier)
    }
}

final class DokusFilenameFormatter {
    private let currencyFormatter: NumberFormatter = {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.locale = Locale(identifier: "nl_BE")
        formatter.currencyCode = "EUR"
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 2
        return formatter
    }()

    func filename(for record: DokusDocumentRecord, placement: DokusPlacement) -> String {
        switch placement {
        case .inbox:
            return sanitizeFilename(record.originalFilename)
        case .needsReview:
            return needsReviewFilename(record: record)
        case .typed(let folder):
            return typedFilename(record: record, folder: folder)
        }
    }

    private func needsReviewFilename(record: DokusDocumentRecord) -> String {
        let ext = extensionPart(from: record)
        let typeLabel = record.draftType?.rawValue ?? DokusDocumentType.unknown.rawValue
        let counterparty = record.counterpartyName?.trimmingCharacters(in: .whitespacesAndNewlines)

        var components = [typeLabel]
        if let counterparty, !counterparty.isEmpty {
            components.append(counterparty)
        } else {
            components.append(URL(fileURLWithPath: record.originalFilename).deletingPathExtension().lastPathComponent)
        }
        let name = components.joined(separator: " - ")
        return sanitizeFilename("\(name).\(ext)")
    }

    private func typedFilename(record: DokusDocumentRecord, folder: DokusTypedFolder) -> String {
        let ext = extensionPart(from: record)
        let counterparty = record.counterpartyName?.trimmingCharacters(in: .whitespacesAndNewlines)
        let amount = amountLabel(record.amountMinor)
        let number = record.documentNumber?.trimmingCharacters(in: .whitespacesAndNewlines)

        let prefix: String
        let includeAmount: Bool
        switch folder {
        case .invoicesIn, .invoicesOut:
            prefix = number.map { "INV-\($0)" } ?? "INVOICE"
            includeAmount = true
        case .creditNotesIn, .creditNotesOut:
            prefix = number.map { "CN-\($0)" } ?? "CREDIT_NOTE"
            includeAmount = true
        case .receiptsIn, .receiptsOut:
            prefix = number.map { "REC-\($0)" } ?? "RECEIPT"
            includeAmount = true
        case .quotes:
            prefix = number.map { "QUO-\($0)" } ?? "QUOTE"
            includeAmount = false
        case .proForma:
            prefix = number.map { "PRO-\($0)" } ?? "PRO_FORMA"
            includeAmount = false
        case .exports:
            return sanitizeFilename(record.originalFilename)
        }

        var parts = [prefix]
        if let counterparty, !counterparty.isEmpty {
            parts.append(counterparty)
        }
        if includeAmount, let amount, !amount.isEmpty {
            parts.append(amount)
        }

        let basename = parts.joined(separator: " - ")
        return sanitizeFilename("\(basename).\(ext)")
    }

    private func amountLabel(_ minor: Int64?) -> String? {
        guard let minor else { return nil }
        let major = Double(minor) / 100.0
        return currencyFormatter.string(from: NSNumber(value: major))
    }

    private func extensionPart(from record: DokusDocumentRecord) -> String {
        let originalExt = URL(fileURLWithPath: record.originalFilename).pathExtension
        if !originalExt.isEmpty {
            return originalExt
        }
        return UTType.fromMimeType(record.contentType).preferredFilenameExtension ?? "bin"
    }

    private func sanitizeFilename(_ raw: String) -> String {
        var sanitized = raw.replacingOccurrences(of: "/", with: "_")
        sanitized = sanitized.replacingOccurrences(of: ":", with: "-")
        return sanitized
    }
}
