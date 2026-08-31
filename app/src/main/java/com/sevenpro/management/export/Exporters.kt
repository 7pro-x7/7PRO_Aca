package com.sevenpro.management.export

import android.content.Context
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.UnitValue
import com.sevenpro.management.data.model.FinancialReport
import com.opencsv.CSVWriter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    fun exportFinancialReport(context: Context, report: FinancialReport): File {
        val file = File(context.cacheDir, "7PRO_Financial_Report_${report.periodStart}.pdf")
        val writer = PdfWriter(file)
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument)

        // Header
        document.add(Paragraph("7PRO Management - Financial Report").setFontSize(20f).setBold())
        document.add(Paragraph("Period: ${report.periodStart} to ${report.periodEnd}").setFontSize(12f))
        document.add(Paragraph(" "))

        // Summary Table
        val summaryData = arrayOf(
            arrayOf("Item", "Amount"),
            arrayOf("Total Revenue", "$${String.format("%.2f", report.totalRevenue)}"),
            arrayOf("Total Expenses", "$${String.format("%.2f", report.totalExpenses)}"),
            arrayOf("Teacher Earnings", "$${String.format("%.2f", report.teacherEarnings)}"),
            arrayOf("Net Profit", "$${String.format("%.2f", report.netProfit)}"),
            arrayOf("Subscriptions", "${report.subscriptionCount}")
        )

        val table = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)).useAllAvailableWidth())
        for (row in summaryData) {
            for (cell in row) {
                table.addCell(cell)
            }
        }
        document.add(table)
        document.add(Paragraph(" "))

        // Category Breakdown
        if (report.paymentBreakdown.isNotEmpty()) {
            document.add(Paragraph("Category Breakdown").setFontSize(14f).setBold())
            val catTable = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f)).useAllAvailableWidth())
            catTable.addCell("Category")
            catTable.addCell("Amount")
            for ((cat, amount) in report.paymentBreakdown) {
                catTable.addCell(cat.ifEmpty { "Uncategorized" })
                catTable.addCell("$${String.format("%.2f", amount)}")
            }
            document.add(catTable)
        }

        document.close()
        return file
    }
}

object CsvExporter {

    fun exportFinancialReport(context: Context, report: FinancialReport): File {
        val file = File(context.cacheDir, "7PRO_Financial_Report_${report.periodStart}.csv")
        val writer = CSVWriter(file.writer())

        // Header
        writer.writeNext(arrayOf("7PRO Management - Financial Report"))
        writer.writeNext(arrayOf("Period", "${report.periodStart} to ${report.periodEnd}"))
        writer.writeNext(arrayOf(""))

        // Summary
        writer.writeNext(arrayOf("Item", "Amount"))
        writer.writeNext(arrayOf("Total Revenue", String.format("%.2f", report.totalRevenue)))
        writer.writeNext(arrayOf("Total Expenses", String.format("%.2f", report.totalExpenses)))
        writer.writeNext(arrayOf("Teacher Earnings", String.format("%.2f", report.teacherEarnings)))
        writer.writeNext(arrayOf("Net Profit", String.format("%.2f", report.netProfit)))
        writer.writeNext(arrayOf("Subscriptions", report.subscriptionCount.toString()))
        writer.writeNext(arrayOf(""))

        // Transactions
        if (report.transactions.isNotEmpty()) {
            writer.writeNext(arrayOf("Transactions"))
            writer.writeNext(arrayOf("Type", "Amount", "Description", "Category", "Date"))
            for (txn in report.transactions) {
                writer.writeNext(arrayOf(
                    txn.type,
                    String.format("%.2f", txn.amount),
                    txn.description,
                    txn.category,
                    txn.created_at.take(10)
                ))
            }
        }

        writer.close()
        return file
    }
}

object ExcelExporter {

    fun exportFinancialReport(context: Context, report: FinancialReport): File {
        val file = File(context.cacheDir, "7PRO_Financial_Report_${report.periodStart}.xlsx")
        val workbook = XSSFWorkbook()

        // Summary Sheet
        val summarySheet = workbook.createSheet("Summary")
        val headerRow = summarySheet.createRow(0)
        headerRow.createCell(0).setCellValue("7PRO Management - Financial Report")
        headerRow.getCell(0).cellStyle = workbook.createCellStyle().apply {
            font = workbook.createFont().apply { bold = true; fontHeightInPoints = 14 }
        }

        val periodRow = summarySheet.createRow(1)
        periodRow.createCell(0).setCellValue("Period")
        periodRow.createCell(1).setCellValue("${report.periodStart} to ${report.periodEnd}")

        val dataRows = listOf(
            Triple(3, "Total Revenue", report.totalRevenue),
            Triple(4, "Total Expenses", report.totalExpenses),
            Triple(5, "Teacher Earnings", report.teacherEarnings),
            Triple(6, "Net Profit", report.netProfit)
        )
        for ((rowNum, label, amount) in dataRows) {
            val row = summarySheet.createRow(rowNum)
            row.createCell(0).setCellValue(label)
            row.createCell(1).setCellValue(amount)
        }

        summarySheet.createRow(8).createCell(0).setCellValue("Subscriptions")
        summarySheet.createRow(8).createCell(1).setCellValue(report.subscriptionCount.toDouble())

        // Category Breakdown Sheet
        if (report.paymentBreakdown.isNotEmpty()) {
            val catSheet = workbook.createSheet("Categories")
            val catHeader = catSheet.createRow(0)
            catHeader.createCell(0).setCellValue("Category")
            catHeader.createCell(1).setCellValue("Amount")

            var rowNum = 1
            for ((cat, amount) in report.paymentBreakdown) {
                val row = catSheet.createRow(rowNum++)
                row.createCell(0).setCellValue(cat.ifEmpty { "Uncategorized" })
                row.createCell(1).setCellValue(amount)
            }
        }

        // Transactions Sheet
        if (report.transactions.isNotEmpty()) {
            val txnSheet = workbook.createSheet("Transactions")
            val txnHeader = txnSheet.createRow(0)
            txnHeader.createCell(0).setCellValue("Type")
            txnHeader.createCell(1).setCellValue("Amount")
            txnHeader.createCell(2).setCellValue("Description")
            txnHeader.createCell(3).setCellValue("Category")
            txnHeader.createCell(4).setCellValue("Date")

            var rowNum = 1
            for (txn in report.transactions) {
                val row = txnSheet.createRow(rowNum++)
                row.createCell(0).setCellValue(txn.type)
                row.createCell(1).setCellValue(txn.amount)
                row.createCell(2).setCellValue(txn.description)
                row.createCell(3).setCellValue(txn.category)
                row.createCell(4).setCellValue(txn.created_at.take(10))
            }
        }

        val outputStream = FileOutputStream(file)
        workbook.write(outputStream)
        workbook.close()
        outputStream.close()
        return file
    }
}
