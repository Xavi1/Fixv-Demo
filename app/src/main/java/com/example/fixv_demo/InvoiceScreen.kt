package com.example.fixv_demo

import android.content.Intent
import android.os.Environment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.style.TextOverflow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(invoiceJson: String?) {
    val context = LocalContext.current
    val invoiceDetails = Gson().fromJson(invoiceJson, InvoiceDetails::class.java)

    val dateFormat = SimpleDateFormat("EEE MMM dd", Locale.getDefault())
    val createdAtDate = dateFormat.parse(invoiceDetails.createdAt)
    val formattedDate = dateFormat.format(createdAtDate)

    var showConfirmationDialog by remember { mutableStateOf(false) }

    // Determine text color based on theme
    val textColor = if (isSystemInDarkTheme()) Color.White else Color.DarkGray

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.fixv_logo),
                        contentDescription = "Logo"
                    )
                },
                actions = {
                    IconButton(onClick = { showConfirmationDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.download),
                            contentDescription = "Download PDF"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Divider(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth(0.8f),
                thickness = 1.dp,
                color = if (isSystemInDarkTheme()) Color.Gray else Color.LightGray
            )

            Text(
                text = "INVOICE",
                fontSize = 36.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 8.sp,
                modifier = Modifier.padding(vertical = 24.dp),
                color = textColor
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "ISSUED TO:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    Text(
                        text = "${invoiceDetails.userName}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "VEHICLE DETAILS:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    Text(
                        text = "${invoiceDetails.vehicleDetails.split(" ")[0]}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor
                    )
                    Text(
                        text = "${invoiceDetails.vehicleDetails.split(" ").drop(1).joinToString(" ")}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "PAY TO:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    Text(
                        text = "${invoiceDetails.shopName}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor
                    )

                    Text(
                        text = "Account Name: FIXV",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor
                    )

                    Text(
                        text = "Services: ${invoiceDetails.services.joinToString(", ")}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor
                    )

                    Text(
                        text = "Shop ID: ${invoiceDetails.shopId}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text(
                            text = "INVOICE NO:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        Text(
                            text = "${invoiceDetails.invoiceId}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = textColor,
                            textAlign = TextAlign.End
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text(
                            text = "CURRENT DATE:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        Text(
                            text = formattedDate,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = textColor,
                            textAlign = TextAlign.End
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text(
                            text = "DUE DATE:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        Text(
                            text = "${invoiceDetails.appointmentDate}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = textColor,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            InvoiceServicesTable(invoiceDetails, textColor)

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(0.4f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUBTOTAL",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )

                    Text(
                        text = "$${calculateSubtotal(invoiceDetails).toInt()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor,
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(0.4f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tax",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "10%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor,
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(0.4f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "$${invoiceDetails.totalAmount}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    val intent = Intent(context, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text(text = "Back to Home", color = Color.White)
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (showConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmationDialog = false },
                    title = { Text("Download PDF") },
                    text = { Text("Are you sure you want to download the invoice as a PDF?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showConfirmationDialog = false
                            saveInvoiceAsPdf(invoiceDetails)
                        }) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmationDialog = false }) {
                            Text("No")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun InvoiceServicesTable(invoiceDetails: InvoiceDetails, textColor: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "DESCRIPTION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.weight(2f)
            )

            Text(
                text = "UNIT PRICE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "QTY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.5f)
            )

            Text(
                text = "TOTAL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }

        Divider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = if (isSystemInDarkTheme()) Color.Gray else Color.LightGray
        )

        invoiceDetails.services.forEach { service ->
            val price = invoiceDetails.servicePrices[service] ?: 0.0
            val formattedPrice = if (price > 0) "$${price.toInt()}" else "-"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = service,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = textColor,
                    modifier = Modifier.weight(2f)
                )

                Text(
                    text = "$${price.toInt()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "1",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.5f)
                )

                Text(
                    text = formattedPrice,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = textColor,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Divider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = if (isSystemInDarkTheme()) Color.Gray else Color.LightGray
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}

// Helper function to calculate subtotal
private fun calculateSubtotal(invoiceDetails: InvoiceDetails): Double {
    return invoiceDetails.totalAmount / 1.1 // Assuming 10% fee
}

// Save As Pdf function
fun saveInvoiceAsPdf(invoiceDetails: InvoiceDetails) {
    val pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
    val file = File(pdfPath, "invoice_${invoiceDetails.invoiceId}.pdf")
    val writer = PdfWriter(file)
    val pdfDocument = com.itextpdf.kernel.pdf.PdfDocument(writer)
    val document = Document(pdfDocument)

    document.add(Paragraph("INVOICE"))
    document.add(Paragraph("Invoice No: ${invoiceDetails.invoiceId}"))
    document.add(Paragraph("Issued To: ${invoiceDetails.userName}"))
    document.add(Paragraph("Vehicle Details: ${invoiceDetails.vehicleDetails}"))
    document.add(Paragraph("Pay To: ${invoiceDetails.shopName}"))
    document.add(Paragraph("Shop ID: ${invoiceDetails.shopId}"))
    document.add(Paragraph("Date: ${invoiceDetails.createdAt}"))
    document.add(Paragraph("Due Date: ${invoiceDetails.appointmentDate}"))

    document.add(Paragraph("Services:"))
    invoiceDetails.services.forEach { service ->
        val price = invoiceDetails.servicePrices[service] ?: 0.0
        document.add(Paragraph("$service: $${price.toInt()}"))
    }

    document.add(Paragraph("Total Amount: $${invoiceDetails.totalAmount}"))

    document.close()
}

data class InvoiceDetails(
    val invoiceId: String,
    val totalAmount: Double,
    val userName: String,
    val paymentStatus: String,
    val appointmentDate: String,
    val createdAt: String,
    val shopName: String,
    val services: List<String>,
    val vehicleDetails: String,
    val shopId: String,
    val servicePrices: Map<String, Double>
)