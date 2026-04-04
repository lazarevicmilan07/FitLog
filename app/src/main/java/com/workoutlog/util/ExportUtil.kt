package com.workoutlog.util

import android.content.Context
import android.net.Uri
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.FontFactory
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.workoutlog.domain.model.MonthlyReport
import com.workoutlog.domain.model.YearlyReport
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

object ExportUtil {

    // ─── PDF color palette ───────────────────────────────────────────────────

    private val C_PRIMARY  = BaseColor(22,  46,  80)   // dark navy  – header bg
    private val C_ACCENT   = BaseColor(37, 117, 182)   // steel blue – section bars & table headers
    private val C_CARD_A   = BaseColor(37, 117, 182)   // workouts card
    private val C_CARD_B   = BaseColor(22, 140, 100)   // rest days card
    private val C_CARD_C   = BaseColor(200, 100,  10)  // duration card
    private val C_CARD_D   = BaseColor(185,  43,  43)  // calories card
    private val C_HDR_BG   = BaseColor(232, 242, 252)  // section header background
    private val C_ROW_ALT  = BaseColor(244, 248, 253)  // alternating table row
    private val C_BORDER   = BaseColor(200, 215, 230)  // row separator
    private val C_TEXT     = BaseColor(30,   41,  59)  // body text

    // ─── Excel ──────────────────────────────────────────────────────────────

    private data class XlStyles(
        val title: XSSFCellStyle,
        val section: XSSFCellStyle,
        val tblHeader: XSSFCellStyle,
        val metricLabel: XSSFCellStyle,
        val metricValue: XSSFCellStyle,
        val row: XSSFCellStyle,
        val rowAlt: XSSFCellStyle,
        val rowLeft: XSSFCellStyle,
        val rowAltLeft: XSSFCellStyle
    )

    private fun buildXlStyles(wb: XSSFWorkbook): XlStyles {
        fun font(bold: Boolean = false, size: Int = 11, color: Short = IndexedColors.AUTOMATIC.index) =
            wb.createFont().apply {
                this.bold = bold
                fontHeightInPoints = size.toShort()
                this.color = color
            }

        fun style(
            fill: Short? = null,
            h: HorizontalAlignment = HorizontalAlignment.LEFT,
            bold: Boolean = false,
            size: Int = 11,
            fontColor: Short = IndexedColors.AUTOMATIC.index,
            borders: Boolean = false,
            indent: Int = 0
        ) = wb.createCellStyle().apply {
            if (fill != null) {
                fillForegroundColor = fill
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }
            alignment = h
            verticalAlignment = VerticalAlignment.CENTER
            setFont(font(bold, size, fontColor))
            if (borders) {
                setBorderTop(BorderStyle.THIN)
                setBorderBottom(BorderStyle.THIN)
                setBorderLeft(BorderStyle.THIN)
                setBorderRight(BorderStyle.THIN)
            }
            indention = indent.toShort()
        }

        val white = IndexedColors.WHITE.index
        return XlStyles(
            title       = style(fill = IndexedColors.DARK_BLUE.index,               h = HorizontalAlignment.CENTER, bold = true, size = 16, fontColor = white),
            section     = style(fill = IndexedColors.CORNFLOWER_BLUE.index,          bold = true, size = 12, fontColor = white, indent = 1),
            tblHeader   = style(fill = IndexedColors.SKY_BLUE.index,                 h = HorizontalAlignment.CENTER, bold = true, size = 11, fontColor = white, borders = true),
            metricLabel = style(fill = IndexedColors.LIGHT_CORNFLOWER_BLUE.index,    borders = true, indent = 1),
            metricValue = style(h = HorizontalAlignment.RIGHT,                       bold = true, size = 12, borders = true),
            row         = style(h = HorizontalAlignment.CENTER,                      borders = true),
            rowAlt      = style(fill = IndexedColors.PALE_BLUE.index,                h = HorizontalAlignment.CENTER, borders = true),
            rowLeft     = style(borders = true, indent = 1),
            rowAltLeft  = style(fill = IndexedColors.PALE_BLUE.index,                borders = true, indent = 1)
        )
    }

    /** Creates a merged row with a solid-color section heading. Every cell in
     *  the range gets the same style so borders render across the full width. */
    private fun xlMergedRow(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        rowIdx: Int, c0: Int, c1: Int,
        text: String, style: XSSFCellStyle, heightPt: Float
    ) {
        val row = sheet.createRow(rowIdx).apply { setHeightInPoints(heightPt) }
        row.createCell(c0).apply { setCellValue(text); cellStyle = style }
        for (c in c0 + 1..c1) row.createCell(c).cellStyle = style
        if (c1 > c0) sheet.addMergedRegion(CellRangeAddress(rowIdx, rowIdx, c0, c1))
    }

    fun exportMonthlyToExcel(context: Context, uri: Uri, reports: List<MonthlyReport>) {
        val wb = XSSFWorkbook()
        val s = buildXlStyles(wb)
        reports.forEach { report -> addMonthlySheet(wb, s, report) }
        context.contentResolver.openOutputStream(uri)?.use { os -> wb.write(os); wb.close() }
    }

    fun exportYearlyToExcel(context: Context, uri: Uri, reports: List<YearlyReport>) {
        val wb = XSSFWorkbook()
        val s = buildXlStyles(wb)
        reports.forEach { report -> addYearlySheet(wb, s, report) }
        context.contentResolver.openOutputStream(uri)?.use { os -> wb.write(os); wb.close() }
    }

    private fun addMonthlySheet(wb: XSSFWorkbook, s: XlStyles, report: MonthlyReport) {
        val ym = YearMonth.of(report.year, report.month)
        val sheetName = "${ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${ym.year}"
        val sheet = wb.createSheet(sheetName)
        var r = 0

        xlMergedRow(sheet, r++, 0, 2,
            "${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}  ·  Workout Report",
            s.title, 45f)
        sheet.createRow(r++)

        xlMergedRow(sheet, r++, 0, 2, "KEY METRICS", s.section, 28f)
        listOf(
            "Total Workouts"        to report.totalWorkouts.toString(),
            "Rest Days"             to report.totalRestDays.toString(),
            "Total Duration (min)"  to report.totalDuration.toString(),
            "Total Calories"        to report.totalCalories.toString()
        ).forEach { (label, value) ->
            val row = sheet.createRow(r).apply { setHeightInPoints(24f) }
            row.createCell(0).apply { setCellValue(label); cellStyle = s.metricLabel }
            row.createCell(1).apply { cellStyle = s.metricLabel }
            row.createCell(2).apply { setCellValue(value); cellStyle = s.metricValue }
            sheet.addMergedRegion(CellRangeAddress(r, r, 0, 1))
            r++
        }
        sheet.createRow(r++)

        if (report.workoutTypeCounts.isNotEmpty()) {
            xlMergedRow(sheet, r++, 0, 2, "WORKOUT DISTRIBUTION", s.section, 28f)
            sheet.createRow(r++).apply {
                setHeightInPoints(24f)
                createCell(0).apply { setCellValue("Workout Type"); cellStyle = s.tblHeader }
                createCell(1).apply { setCellValue("Count");         cellStyle = s.tblHeader }
                createCell(2).apply { setCellValue("Percentage");    cellStyle = s.tblHeader }
            }
            val total = report.workoutTypeCounts.sumOf { it.count }.toFloat()
            report.workoutTypeCounts.forEachIndexed { i, tc ->
                val alt = i % 2 == 1
                sheet.createRow(r++).apply {
                    setHeightInPoints(22f)
                    createCell(0).apply { setCellValue(tc.workoutType.name);                      cellStyle = if (alt) s.rowAltLeft else s.rowLeft }
                    createCell(1).apply { setCellValue(tc.count.toDouble());                      cellStyle = if (alt) s.rowAlt     else s.row }
                    createCell(2).apply { setCellValue("${((tc.count / total) * 100).toInt()}%"); cellStyle = if (alt) s.rowAlt     else s.row }
                }
            }
        }

        sheet.setColumnWidth(0, 7200); sheet.setColumnWidth(1, 3200); sheet.setColumnWidth(2, 3800)
    }

    private fun addYearlySheet(wb: XSSFWorkbook, s: XlStyles, report: YearlyReport) {
        val sheet = wb.createSheet("${report.year}")
        var r = 0

        xlMergedRow(sheet, r++, 0, 1, "${report.year}  ·  Yearly Workout Report", s.title, 45f)
        sheet.createRow(r++)

        xlMergedRow(sheet, r++, 0, 1, "KEY METRICS", s.section, 28f)
        listOf(
            "Total Workouts" to report.totalWorkouts.toString(),
            "Rest Days"      to report.totalRestDays.toString()
        ).forEach { (label, value) ->
            sheet.createRow(r++).apply {
                setHeightInPoints(24f)
                createCell(0).apply { setCellValue(label); cellStyle = s.metricLabel }
                createCell(1).apply { setCellValue(value); cellStyle = s.metricValue }
            }
        }
        sheet.createRow(r++)

        xlMergedRow(sheet, r++, 0, 1, "MONTHLY BREAKDOWN", s.section, 28f)
        sheet.createRow(r++).apply {
            setHeightInPoints(24f)
            createCell(0).apply { setCellValue("Month");    cellStyle = s.tblHeader }
            createCell(1).apply { setCellValue("Workouts"); cellStyle = s.tblHeader }
        }
        report.monthlyCounts.forEachIndexed { i, mc ->
            val alt = i % 2 == 1
            sheet.createRow(r++).apply {
                setHeightInPoints(22f)
                createCell(0).apply {
                    setCellValue(Month.of(mc.month).getDisplayName(TextStyle.FULL, Locale.getDefault()))
                    cellStyle = if (alt) s.rowAltLeft else s.rowLeft
                }
                createCell(1).apply { setCellValue(mc.count.toDouble()); cellStyle = if (alt) s.rowAlt else s.row }
            }
        }
        sheet.createRow(r++)

        if (report.workoutTypeCounts.isNotEmpty()) {
            xlMergedRow(sheet, r++, 0, 1, "WORKOUT DISTRIBUTION", s.section, 28f)
            sheet.createRow(r++).apply {
                setHeightInPoints(24f)
                createCell(0).apply { setCellValue("Workout Type"); cellStyle = s.tblHeader }
                createCell(1).apply { setCellValue("Count");        cellStyle = s.tblHeader }
            }
            report.workoutTypeCounts.forEachIndexed { i, tc ->
                val alt = i % 2 == 1
                sheet.createRow(r++).apply {
                    setHeightInPoints(22f)
                    createCell(0).apply { setCellValue(tc.workoutType.name);  cellStyle = if (alt) s.rowAltLeft else s.rowLeft }
                    createCell(1).apply { setCellValue(tc.count.toDouble()); cellStyle = if (alt) s.rowAlt     else s.row }
                }
            }
        }

        sheet.setColumnWidth(0, 7200); sheet.setColumnWidth(1, 3200)
    }

    // ─── PDF ────────────────────────────────────────────────────────────────

    fun exportMonthlyToPdf(context: Context, uri: Uri, reports: List<MonthlyReport>) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            val doc = Document(PageSize.A4, 36f, 36f, 36f, 36f)
            PdfWriter.getInstance(doc, os)
            doc.open()
            reports.forEachIndexed { i, report ->
                if (i > 0) doc.newPage()
                addMonthlyReportToDoc(doc, report)
            }
            doc.close()
        }
    }

    fun exportYearlyToPdf(context: Context, uri: Uri, reports: List<YearlyReport>) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            val doc = Document(PageSize.A4, 36f, 36f, 36f, 36f)
            PdfWriter.getInstance(doc, os)
            doc.open()
            reports.forEachIndexed { i, report ->
                if (i > 0) doc.newPage()
                addYearlyReportToDoc(doc, report)
            }
            doc.close()
        }
    }

    private fun addMonthlyReportToDoc(doc: Document, report: MonthlyReport) {
        val ym = YearMonth.of(report.year, report.month)
        val subtitle = "${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}"

        pdfHeader(doc, "Monthly Workout Report", subtitle)

        val cards = PdfPTable(4).apply { widthPercentage = 100f; spacingBefore = 16f; spacingAfter = 20f }
        pdfCard(cards, "WORKOUTS",  "${report.totalWorkouts}", C_CARD_A)
        pdfCard(cards, "REST DAYS", "${report.totalRestDays}", C_CARD_B)
        pdfCard(cards, "MINUTES",   "${report.totalDuration}", C_CARD_C)
        pdfCard(cards, "CALORIES",  "${report.totalCalories}", C_CARD_D)
        doc.add(cards)

        if (report.workoutTypeCounts.isNotEmpty()) {
            pdfSectionTitle(doc, "Workout Distribution")
            val total = report.workoutTypeCounts.sumOf { it.count }.toFloat()
            val tbl = PdfPTable(floatArrayOf(52f, 24f, 24f)).apply { widthPercentage = 100f }
            pdfTblHeader(tbl, "Workout Type", "Count", "Percentage")
            report.workoutTypeCounts.forEachIndexed { i, tc ->
                pdfRow(tbl, i % 2 == 1, tc.workoutType.name, "${tc.count}", "${((tc.count / total) * 100).toInt()}%")
            }
            doc.add(tbl)
        }
    }

    private fun addYearlyReportToDoc(doc: Document, report: YearlyReport) {
        pdfHeader(doc, "Yearly Workout Report", "${report.year}")

        val cards = PdfPTable(2).apply { widthPercentage = 100f; spacingBefore = 16f; spacingAfter = 20f }
        pdfCard(cards, "WORKOUTS",  "${report.totalWorkouts}", C_CARD_A)
        pdfCard(cards, "REST DAYS", "${report.totalRestDays}", C_CARD_B)
        doc.add(cards)

        pdfSectionTitle(doc, "Monthly Breakdown")
        val monthTbl = PdfPTable(floatArrayOf(60f, 40f)).apply { widthPercentage = 100f }
        pdfTblHeader(monthTbl, "Month", "Workouts")
        report.monthlyCounts.forEachIndexed { i, mc ->
            pdfRow(monthTbl, i % 2 == 1, Month.of(mc.month).getDisplayName(TextStyle.FULL, Locale.getDefault()), "${mc.count}")
        }
        doc.add(monthTbl)

        if (report.workoutTypeCounts.isNotEmpty()) {
            pdfSectionTitle(doc, "Workout Distribution")
            val distTbl = PdfPTable(floatArrayOf(70f, 30f)).apply { widthPercentage = 100f }
            pdfTblHeader(distTbl, "Workout Type", "Count")
            report.workoutTypeCounts.forEachIndexed { i, tc ->
                pdfRow(distTbl, i % 2 == 1, tc.workoutType.name, "${tc.count}")
            }
            doc.add(distTbl)
        }
    }

    // ─── PDF building blocks ─────────────────────────────────────────────────

    /** Full-width dark header with a large title and a lighter subtitle. */
    private fun pdfHeader(doc: Document, title: String, subtitle: String) {
        val tbl = PdfPTable(1).apply { widthPercentage = 100f }
        val cell = PdfPCell().apply {
            backgroundColor = C_PRIMARY
            border = Rectangle.NO_BORDER
            paddingTop = 22f; paddingBottom = 22f; paddingLeft = 16f; paddingRight = 16f
        }
        cell.addElement(Paragraph(title,
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20f, BaseColor.WHITE)
        ).apply { alignment = Element.ALIGN_CENTER })
        cell.addElement(Paragraph(subtitle,
            FontFactory.getFont(FontFactory.HELVETICA, 13f, BaseColor(175, 210, 245))
        ).apply { alignment = Element.ALIGN_CENTER; spacingBefore = 6f })
        tbl.addCell(cell)
        doc.add(tbl)
    }

    /** Accent-bar section heading: thin coloured left stripe + light background label. */
    private fun pdfSectionTitle(doc: Document, text: String) {
        val tbl = PdfPTable(floatArrayOf(3f, 97f)).apply {
            widthPercentage = 100f; spacingBefore = 18f; spacingAfter = 6f
        }
        tbl.addCell(PdfPCell().apply {
            backgroundColor = C_ACCENT; border = Rectangle.NO_BORDER; minimumHeight = 30f
        })
        tbl.addCell(PdfPCell(Phrase(text,
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, C_TEXT)
        )).apply {
            backgroundColor = C_HDR_BG; border = Rectangle.NO_BORDER
            verticalAlignment = Element.ALIGN_MIDDLE
            paddingTop = 7f; paddingBottom = 7f; paddingLeft = 10f
        })
        doc.add(tbl)
    }

    /** Coloured metric card: big number on top, small uppercase label below. */
    private fun pdfCard(table: PdfPTable, label: String, value: String, bg: BaseColor) {
        val cell = PdfPCell().apply {
            backgroundColor = bg
            border = Rectangle.BOX; borderColor = BaseColor.WHITE; borderWidth = 2f
            paddingTop = 16f; paddingBottom = 16f; paddingLeft = 6f; paddingRight = 6f
        }
        cell.addElement(Paragraph(value,
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22f, BaseColor.WHITE)
        ).apply { alignment = Element.ALIGN_CENTER })
        cell.addElement(Paragraph(label,
            FontFactory.getFont(FontFactory.HELVETICA, 8f, BaseColor(210, 235, 255))
        ).apply { alignment = Element.ALIGN_CENTER; spacingBefore = 4f })
        table.addCell(cell)
    }

    /** Steel-blue header row for a data table. Accepts any number of column titles. */
    private fun pdfTblHeader(table: PdfPTable, vararg headers: String) {
        headers.forEach { h ->
            table.addCell(PdfPCell(Phrase(h,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, BaseColor.WHITE)
            )).apply {
                backgroundColor = C_ACCENT; border = Rectangle.NO_BORDER
                horizontalAlignment = Element.ALIGN_CENTER
                paddingTop = 9f; paddingBottom = 9f; paddingLeft = 6f; paddingRight = 6f
            })
        }
    }

    /** Striped data row: first cell is left-aligned, remaining cells are centred.
     *  Only a bottom separator line is drawn so the table looks open and clean. */
    private fun pdfRow(table: PdfPTable, alt: Boolean, vararg cells: String) {
        val bg = if (alt) C_ROW_ALT else BaseColor.WHITE
        cells.forEachIndexed { i, text ->
            table.addCell(PdfPCell(Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA, 10f, C_TEXT)
            )).apply {
                backgroundColor = bg
                border = Rectangle.BOTTOM; borderWidthBottom = 0.5f; borderColor = C_BORDER
                horizontalAlignment = if (i == 0) Element.ALIGN_LEFT else Element.ALIGN_CENTER
                paddingTop = 7f; paddingBottom = 7f
                paddingLeft = if (i == 0) 8f else 4f; paddingRight = 4f
            })
        }
    }
}
