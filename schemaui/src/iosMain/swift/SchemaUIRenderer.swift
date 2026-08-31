// SchemaUIRenderer.swift
// Add this file to your Xcode project alongside the schemauiKit.xcframework.
//
// Minimum deployment target: iOS 16+
// Swift 5.9+
//
// Usage:
//   let kit = SchemaUIKit()
//   kit.registerAction(name: "submit") {
//       let state = kit.stateStore.snapshot() as! [String: String]
//       print("Submit tapped, state:", state)
//   }
//   if let node = kit.parseSchema(json: myJsonString) {
//       SchemaUIView(node: node, kit: kit)
//   }

import SwiftUI
import schemauiKit

// ─── Public Entry Point ───────────────────────────────────────────────────────

/// Top-level SwiftUI view that renders a KMP UINode tree at runtime.
public struct SchemaUIView: View {
    let node: UINode
    let kit: SchemaUIKit
    let customNodeRenderer: ((UIUnknown) -> AnyView)?

    public init(
        node: UINode,
        kit: SchemaUIKit,
        customNodeRenderer: ((UIUnknown) -> AnyView)? = nil
    ) {
        self.node = node
        self.kit = kit
        self.customNodeRenderer = customNodeRenderer
    }

    public var body: some View {
        renderNode(node)
    }

    // MARK: - Recursive renderer

    @ViewBuilder
    func renderNode(_ node: UINode) -> some View {
        switch node {
        case let columnNode as UIColumn:
            renderColumn(columnNode)
        case let rowNode as UIRow:
            renderRow(rowNode)
        case let boxNode as UIBox:
            renderBox(boxNode)
        case let textNode as UIText:
            renderText(textNode)
        case let imageNode as UIImage:
            renderImage(imageNode)
        case let buttonNode as UIButton:
            renderButton(buttonNode)
        case let textFieldNode as UITextField:
            renderTextField(textFieldNode)
        case let spacerNode as UISpacer:
            renderSpacer(spacerNode)
        case let listNode as UIList:
            renderList(listNode)
        case let unknownNode as UIUnknown:
            if let customView = customNodeRenderer?(unknownNode) {
                customView
            } else {
                Text("⚠ Unknown: \(unknownNode.originalType)")
                    .foregroundColor(.orange)
            }
        default:
            EmptyView()
        }
    }

    // ─── Containers ──────────────────────────────────────────────────────────

    @ViewBuilder
    func renderColumn(_ node: UIColumn) -> some View {
        VStack(
            alignment: node.horizontalAlignment.toSwiftUIHorizontalAlignment(),
            spacing: 0
        ) {
            ForEach(Array(node.children.enumerated()), id: \.offset) { _, child in
                renderNode(child)
            }
        }
        .applyModifiers(node.modifiers)
    }

    @ViewBuilder
    func renderRow(_ node: UIRow) -> some View {
        HStack(
            alignment: node.verticalAlignment.toSwiftUIVerticalAlignment(),
            spacing: 0
        ) {
            ForEach(Array(node.children.enumerated()), id: \.offset) { _, child in
                renderNode(child)
            }
        }
        .applyModifiers(node.modifiers)
    }

    @ViewBuilder
    func renderBox(_ node: UIBox) -> some View {
        ZStack(alignment: node.contentAlignment.toSwiftUIAlignment()) {
            ForEach(Array(node.children.enumerated()), id: \.offset) { _, child in
                renderNode(child)
            }
        }
        .applyModifiers(node.modifiers)
    }

    // ─── Leaf Nodes ──────────────────────────────────────────────────────────

    @ViewBuilder
    func renderText(_ node: UIText) -> some View {
        Text(node.text)
            .font(.system(size: CGFloat(node.style.fontSize)))
            .fontWeight(node.style.fontWeight.toSwiftUI())
            .italic(node.style.fontStyle == .italic)
            .foregroundColor(node.style.color.flatMap { Color(hex: $0) })
            .multilineTextAlignment(node.style.textAlign.toSwiftUI())
            .lineLimit(node.style.maxLines == Int32.max ? nil : Int(node.style.maxLines))
            .applyModifiers(node.modifiers)
    }

    @ViewBuilder
    func renderImage(_ node: UIImage) -> some View {
        let urlString = node.url ?? node.resource ?? ""
        if let url = URL(string: urlString), urlString.hasPrefix("http") {
            AsyncImage(url: url) { phase in
                switch phase {
                case .empty:
                    ProgressView()
                case .success(let image):
                    image.resizable()
                        .aspectRatio(contentMode: node.contentScale.toSwiftUI())
                case .failure:
                    Image(systemName: "photo")
                        .foregroundColor(.secondary)
                @unknown default:
                    EmptyView()
                }
            }
            .applyModifiers(node.modifiers)
        } else {
            Image(urlString)
                .resizable()
                .aspectRatio(contentMode: node.contentScale.toSwiftUI())
                .applyModifiers(node.modifiers)
        }
    }

    @ViewBuilder
    func renderButton(_ node: UIButton) -> some View {
        let action = { kit.triggerAction(name: node.action) }
        Group {
            switch node.style {
            case .filled:
                Button(node.label, action: action)
                    .buttonStyle(.borderedProminent)
            case .outlined:
                Button(node.label, action: action)
                    .buttonStyle(.bordered)
            case .text:
                Button(node.label, action: action)
                    .buttonStyle(.plain)
            case .elevated:
                Button(node.label, action: action)
                    .buttonStyle(.borderedProminent)
                    .shadow(radius: 4)
            case .tonal:
                Button(node.label, action: action)
                    .buttonStyle(.bordered)
                    .tint(.secondary)
            default:
                Button(node.label, action: action)
            }
        }
        .applyModifiers(node.modifiers)
    }

    @ViewBuilder
    func renderTextField(_ node: UITextField) -> some View {
        SchemaUITextFieldView(node: node, kit: kit)
            .applyModifiers(node.modifiers)
    }

    @ViewBuilder
    func renderSpacer(_ node: UISpacer) -> some View {
        if let width = node.width, let height = node.height {
            Spacer().frame(width: CGFloat(truncating: width), height: CGFloat(truncating: height))
        } else if let width = node.width {
            Spacer().frame(width: CGFloat(truncating: width))
        } else if let height = node.height {
            Spacer().frame(height: CGFloat(truncating: height))
        } else {
            Spacer()
        }
    }

    @ViewBuilder
    func renderList(_ node: UIList) -> some View {
        List {
            ForEach(Array(node.items.enumerated()), id: \.offset) { _, item in
                renderNode(item)
                    .listRowSeparator(node.dividers ? .visible : .hidden)
            }
        }
        .listStyle(.plain)
        .applyModifiers(node.modifiers)
    }
}

// ─── TextField View (needs @State) ───────────────────────────────────────────

private struct SchemaUITextFieldView: View {
    let node: UITextField
    let kit: SchemaUIKit

    @State private var text: String = ""

    var body: some View {
        let isSecure = node.inputType == .password
        Group {
            if isSecure {
                SecureField(node.placeholder, text: $text)
                    .textFieldStyle(.roundedBorder)
            } else {
                TextField(node.placeholder, text: $text)
                    .textFieldStyle(.roundedBorder)
                    .keyboardType(node.inputType.toUIKeyboardType())
            }
        }
        .onChange(of: text) { newValue in
            kit.stateStore.set(key: node.stateKey, value: newValue)
        }
        .onSubmit {
            if let action = node.action {
                kit.triggerAction(name: action)
            }
        }
        .onAppear {
            text = kit.stateStore.get(key: node.stateKey) ?? ""
        }
        .overlay(alignment: .topLeading) {
            if !node.label.isEmpty {
                Text(node.label)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .offset(y: -20)
            }
        }
    }
}

private extension View {
    @ViewBuilder
    func applyModifiers(_ modifiers: UIModifiers) -> some View {
        let shape = RoundedRectangle(cornerRadius: CGFloat(modifiers.cornerRadius))
        let backgroundView = modifiers.backgroundColor.flatMap { Color(hex: $0) } ?? Color.clear

        let base = self
            .frame(
                minWidth: modifiers.minWidth.map { CGFloat(truncating: $0) },
                idealWidth: modifiers.width.map { CGFloat(truncating: $0) },
                maxWidth: modifiers.fillMaxWidth ? .infinity : (modifiers.width.map { CGFloat(truncating: $0) }),
                minHeight: modifiers.minHeight.map { CGFloat(truncating: $0) },
                idealHeight: modifiers.height.map { CGFloat(truncating: $0) },
                maxHeight: modifiers.fillMaxHeight ? .infinity : (modifiers.height.map { CGFloat(truncating: $0) })
            )
            // Apply inner padding before background to match Compose semantics
            .padding(EdgeInsets(
                top: CGFloat(modifiers.paddingTop),
                leading: CGFloat(modifiers.paddingStart),
                bottom: CGFloat(modifiers.paddingBottom),
                trailing: CGFloat(modifiers.paddingEnd)
            ))
            .background(backgroundView, in: shape)
            .opacity(Double(modifiers.alpha))

        if modifiers.clip {
            base.clipShape(shape)
        } else {
            base
        }
    }
}

// ─── Enum Converters ──────────────────────────────────────────────────────────

private extension UIHorizontalAlignment {
    func toSwiftUIHorizontalAlignment() -> HorizontalAlignment {
        switch self {
        case .start: return .leading
        case .center: return .center
        case .end: return .trailing
        default: return .leading
        }
    }
}

private extension UIAlignment {
    func toSwiftUIAlignment() -> Alignment {
        switch self {
        case .topStart: return .topLeading
        case .topCenter: return .top
        case .topEnd: return .topTrailing
        case .centerStart: return .leading
        case .center: return .center
        case .centerEnd: return .trailing
        case .bottomStart: return .bottomLeading
        case .bottomCenter: return .bottom
        case .bottomEnd: return .bottomTrailing
        default: return .topLeading
        }
    }
}

private extension UIVerticalAlignment {
    func toSwiftUIVerticalAlignment() -> VerticalAlignment {
        switch self {
        case .top: return .top
        case .center: return .center
        case .bottom: return .bottom
        default: return .top
        }
    }
}

private extension UIFontWeight {
    func toSwiftUI() -> Font.Weight {
        switch self {
        case .thin: return .thin
        case .light: return .light
        case .normal: return .regular
        case .medium: return .medium
        case .semiBold: return .semibold
        case .bold: return .bold
        case .extraBold: return .heavy
        case .black: return .black
        default: return .regular
        }
    }
}

private extension UITextAlign {
    func toSwiftUI() -> TextAlignment {
        switch self {
        case .start: return .leading
        case .center: return .center
        case .end: return .trailing
        case .justify: return .leading // SwiftUI doesn't support justify
        default: return .leading
        }
    }
}

private extension UIContentScale {
    func toSwiftUI() -> ContentMode {
        switch self {
        case .crop: return .fill
        default: return .fit
        }
    }
}

private extension UIInputType {
    func toUIKeyboardType() -> UIKeyboardType {
        switch self {
        case .email: return .emailAddress
        case .number: return .numberPad
        case .phone: return .phonePad
        default: return .default
        }
    }
}

// ─── Color Helper ─────────────────────────────────────────────────────────────

private extension Color {
    /// Initializes a Color from a normalized AARRGGBB hex string (no '#').
    init?(hex: String) {
        guard hex.count == 8,
              let value = UInt64(hex, radix: 16) else { return nil }
        let a = Double((value >> 24) & 0xFF) / 255.0
        let r = Double((value >> 16) & 0xFF) / 255.0
        let g = Double((value >> 8) & 0xFF) / 255.0
        let b = Double(value & 0xFF) / 255.0
        self.init(red: r, green: g, blue: b, opacity: a)
    }
}
