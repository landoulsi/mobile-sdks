package com.landoulsi.schemaui.ir

import com.landoulsi.schemaui.schema.AlignmentModifier
import com.landoulsi.schemaui.schema.BackgroundModifier
import com.landoulsi.schemaui.schema.BoxSchemaNode
import com.landoulsi.schemaui.schema.ButtonSchemaNode
import com.landoulsi.schemaui.schema.ColumnSchemaNode
import com.landoulsi.schemaui.schema.ImageSchemaNode
import com.landoulsi.schemaui.schema.PaddingModifier
import com.landoulsi.schemaui.schema.RowSchemaNode
import com.landoulsi.schemaui.schema.SchemaModifiers
import com.landoulsi.schemaui.schema.SizeModifier
import com.landoulsi.schemaui.schema.TextFieldSchemaNode
import com.landoulsi.schemaui.schema.TextSchemaNode
import com.landoulsi.schemaui.schema.TextStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchemaToIRTest {

    // ─── normalizeHexColor ───────────────────────────────────────────────────

    @Test
    fun `normalizeHexColor with 6 digits adds full alpha`() {
        assertEquals("FFFF0000", "#FF0000".normalizeHexColor())
        assertEquals("FFFF0000", "FF0000".normalizeHexColor())
    }

    @Test
    fun `normalizeHexColor with 8 digits preserves alpha`() {
        assertEquals("80FF0000", "#80FF0000".normalizeHexColor())
        assertEquals("80FF0000", "80FF0000".normalizeHexColor())
    }

    @Test
    fun `normalizeHexColor lowercase converted to uppercase`() {
        assertEquals("FFAABBCC", "#aabbcc".normalizeHexColor())
    }

    @Test
    fun `normalizeHexColor with 3 digits expands to full alpha and RGB`() {
        assertEquals("FFFFFFFF", "#FFF".normalizeHexColor())
        assertEquals("FF112233", "#123".normalizeHexColor())
    }

    @Test
    fun `normalizeHexColor invalid lengths fallback to black`() {
        assertEquals("FF000000", "#FF".normalizeHexColor())
        assertEquals("FF000000", "12345".normalizeHexColor())
        assertEquals("FF000000", "".normalizeHexColor())
    }

    @Test
    fun `toUIModifiers with null returns None`() {
        val modifiers = (null as SchemaModifiers?).toUIModifiers()
        assertEquals(UIModifiers.None, modifiers)
    }

    @Test
    fun `toUIModifiers padding all expands to four sides`() {
        val schema = SchemaModifiers(
            padding = PaddingModifier(all = 12f),
        )
        val modifiers = schema.toUIModifiers()
        assertEquals(12f, modifiers.paddingStart)
        assertEquals(12f, modifiers.paddingEnd)
        assertEquals(12f, modifiers.paddingTop)
        assertEquals(12f, modifiers.paddingBottom)
    }

    @Test
    fun `toUIModifiers padding specific overrides all`() {
        val schema = SchemaModifiers(
            padding = PaddingModifier(all = 8f, top = 20f),
        )
        val modifiers = schema.toUIModifiers()
        assertEquals(20f, modifiers.paddingTop)
        assertEquals(8f, modifiers.paddingBottom)
        assertEquals(8f, modifiers.paddingStart)
        assertEquals(8f, modifiers.paddingEnd)
    }

    @Test
    fun `toUIModifiers horizontal axis overrides all`() {
        val schema = SchemaModifiers(
            padding = PaddingModifier(all = 4f, horizontal = 16f),
        )
        val modifiers = schema.toUIModifiers()
        assertEquals(16f, modifiers.paddingStart)
        assertEquals(16f, modifiers.paddingEnd)
        assertEquals(4f, modifiers.paddingTop)
        assertEquals(4f, modifiers.paddingBottom)
    }

    @Test
    fun `toUIModifiers alpha clamped to 0-1`() {
        val over = SchemaModifiers(alpha = 2.5f)
        val under = SchemaModifiers(alpha = -0.5f)
        assertEquals(1f, over.toUIModifiers().alpha)
        assertEquals(0f, under.toUIModifiers().alpha)
    }

    @Test
    fun `toUIModifiers null size fields remain null`() {
        val schema = SchemaModifiers()
        val modifiers = schema.toUIModifiers()
        assertNull(modifiers.width)
        assertNull(modifiers.height)
    }

    @Test
    fun `toUIModifiers fillMaxWidth set to true`() {
        val schema = SchemaModifiers(
            size = SizeModifier(fillMaxWidth = true),
        )
        val modifiers = schema.toUIModifiers()
        assertTrue(modifiers.fillMaxWidth)
    }

    @Test
    fun `toUIModifiers backgroundColor normalized`() {
        val schema = SchemaModifiers(
            background = BackgroundModifier(color = "#123456"),
        )
        val modifiers = schema.toUIModifiers()
        assertEquals("FF123456", modifiers.backgroundColor)
    }

    @Test
    fun `box alignment 2D mappings`() {
        val boxNode = BoxSchemaNode(
            children = emptyList(),
            modifiers = SchemaModifiers(
                alignment = AlignmentModifier(horizontal = "center", vertical = "bottom"),
            ),
        )
        val uiBox = boxNode.toIR() as UIBox
        assertEquals(UIAlignment.BottomCenter, uiBox.contentAlignment)

        val centerBox = BoxSchemaNode(
            children = emptyList(),
            modifiers = SchemaModifiers(
                alignment = AlignmentModifier(horizontal = "center", vertical = "center"),
            ),
        )
        assertEquals(UIAlignment.Center, (centerBox.toIR() as UIBox).contentAlignment)

        val topEndBox = BoxSchemaNode(
            children = emptyList(),
            modifiers = SchemaModifiers(
                alignment = AlignmentModifier(horizontal = "end", vertical = "top"),
            ),
        )
        assertEquals(UIAlignment.TopEnd, (topEndBox.toIR() as UIBox).contentAlignment)
    }

    @Test
    fun `column arrangements and alignments`() {
        val columnNode = ColumnSchemaNode(
            children = emptyList(),
            verticalArrangement = "spaceBetween",
            modifiers = SchemaModifiers(
                alignment = AlignmentModifier(horizontal = "end"),
            ),
        )
        val uiColumn = columnNode.toIR() as UIColumn
        assertEquals(UIVerticalArrangement.SpaceBetween, uiColumn.verticalArrangement)
        assertEquals(UIHorizontalAlignment.End, uiColumn.horizontalAlignment)
    }

    @Test
    fun `row arrangements and alignments`() {
        val row = RowSchemaNode(
            children = emptyList(),
            horizontalArrangement = "spaceEvenly",
            modifiers = SchemaModifiers(
                alignment = AlignmentModifier(vertical = "bottom"),
            ),
        )
        val uiRow = row.toIR() as UIRow
        assertEquals(UIHorizontalArrangement.SpaceEvenly, uiRow.horizontalArrangement)
        assertEquals(UIVerticalAlignment.Bottom, uiRow.verticalAlignment)
    }

    @Test
    fun `button styles mapping`() {
        val outlined = ButtonSchemaNode(label = "B", action = "a", style = "outlined").toIR() as UIButton
        assertEquals(UIButtonStyle.Outlined, outlined.style)

        val tonal = ButtonSchemaNode(label = "B", action = "a", style = "tonal").toIR() as UIButton
        assertEquals(UIButtonStyle.Tonal, tonal.style)

        val elevated = ButtonSchemaNode(label = "B", action = "a", style = "elevated").toIR() as UIButton
        assertEquals(UIButtonStyle.Elevated, elevated.style)
    }

    @Test
    fun `textField input types mapping`() {
        val emailField = TextFieldSchemaNode(
            label = "L",
            placeholder = "P",
            stateKey = "k",
            inputType = "email",
        ).toIR() as UITextField
        assertEquals(UIInputType.Email, emailField.inputType)

        val passwordField = TextFieldSchemaNode(
            label = "L",
            placeholder = "P",
            stateKey = "k",
            inputType = "password",
        ).toIR() as UITextField
        assertEquals(UIInputType.Password, passwordField.inputType)
    }

    @Test
    fun `image contentScale mapping`() {
        val cropImage = ImageSchemaNode(url = "u", contentScale = "crop").toIR() as UIImage
        assertEquals(UIContentScale.Crop, cropImage.contentScale)

        val fillBoundsImage = ImageSchemaNode(url = "u", contentScale = "fillBounds").toIR() as UIImage
        assertEquals(UIContentScale.FillBounds, fillBoundsImage.contentScale)
    }

    @Test
    fun `text styles font weight and alignment mapping`() {
        val textNode = TextSchemaNode(
            text = "T",
            style = TextStyle(
                fontWeight = "semiBold",
                fontStyle = "italic",
                textAlign = "center",
            ),
        ).toIR() as UIText

        assertEquals(UIFontWeight.SemiBold, textNode.style.fontWeight)
        assertEquals(UIFontStyle.Italic, textNode.style.fontStyle)
        assertEquals(UITextAlign.Center, textNode.style.textAlign)
    }
}
