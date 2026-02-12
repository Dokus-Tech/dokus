import Foundation

struct MultipartPayload {
    let contentType: String
    let body: Data
}

enum MultipartBuilder {
    static func documentUploadBody(fileName: String, mimeType: String, fileData: Data, prefix: String) -> MultipartPayload {
        let boundary = "Boundary-\(UUID().uuidString)"
        var body = Data()

        appendField(name: "prefix", value: prefix, boundary: boundary, to: &body)
        appendFile(name: "file", fileName: fileName, mimeType: mimeType, fileData: fileData, boundary: boundary, to: &body)
        body.append("--\(boundary)--\r\n".utf8Data)

        return MultipartPayload(
            contentType: "multipart/form-data; boundary=\(boundary)",
            body: body
        )
    }

    private static func appendField(name: String, value: String, boundary: String, to body: inout Data) {
        body.append("--\(boundary)\r\n".utf8Data)
        body.append("Content-Disposition: form-data; name=\"\(name)\"\r\n\r\n".utf8Data)
        body.append("\(value)\r\n".utf8Data)
    }

    private static func appendFile(
        name: String,
        fileName: String,
        mimeType: String,
        fileData: Data,
        boundary: String,
        to body: inout Data
    ) {
        body.append("--\(boundary)\r\n".utf8Data)
        body.append("Content-Disposition: form-data; name=\"\(name)\"; filename=\"\(fileName)\"\r\n".utf8Data)
        body.append("Content-Type: \(mimeType)\r\n\r\n".utf8Data)
        body.append(fileData)
        body.append("\r\n".utf8Data)
    }
}

private extension String {
    var utf8Data: Data { Data(utf8) }
}
