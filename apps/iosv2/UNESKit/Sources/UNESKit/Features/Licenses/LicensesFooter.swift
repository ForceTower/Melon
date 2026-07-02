import CoreTransferable
import SwiftUI
import UniformTypeIdentifiers

/// The closing block: the SBOM share row, then the version + compliance
/// signature.
struct LicensesFooter: View {
    var body: some View {
        VStack(spacing: 16) {
            ShareLink(item: SBOMFile(), preview: SharePreview("SBOM · UNES")) {
                shareRow
                    .contentShape(Rectangle())
            }
            .buttonStyle(.pressableCard)

            VStack(spacing: 3) {
                Text(MeFormat.versionBuildLabel)
                    .font(.system(size: 12, weight: .semibold))
                    .tracking(-0.12)
                    .foregroundStyle(UNESColor.ink3)
                Text("Em conformidade com os termos de cada\nlicença reproduzida acima.")
                    .font(.system(size: 11.5, weight: .medium))
                    .lineSpacing(2)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(UNESColor.ink4)
            }
        }
        .padding(EdgeInsets(top: 2, leading: 0, bottom: 12, trailing: 0))
    }

    private var shareRow: some View {
        HStack(spacing: 13) {
            Image(systemName: "square.and.arrow.down")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: 32, height: 32)
                .background(UNESColor.accent, in: RoundedRectangle(cornerRadius: 9, style: .continuous))

            VStack(alignment: .leading, spacing: 1) {
                Text("Baixar SBOM completo")
                    .font(.system(size: 15, weight: .semibold))
                    .tracking(-0.15)
                    .foregroundStyle(UNESColor.ink)
                Text(LicensesSBOM.sizeLabel)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Image(systemName: "chevron.right")
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(UNESColor.ink4)
        }
        .padding(EdgeInsets(top: 13, leading: 14, bottom: 13, trailing: 14))
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }
}

/// The CycloneDX document as a shareable JSON file.
private struct SBOMFile: Transferable {
    static var transferRepresentation: some TransferRepresentation {
        DataRepresentation(exportedContentType: .json) { _ in LicensesSBOM.data }
            .suggestedFileName("unes-sbom.cdx.json")
    }
}

#Preview {
    LicensesFooter()
        .padding(16)
        .frame(maxHeight: .infinity)
        .background(UNESColor.surface)
}
