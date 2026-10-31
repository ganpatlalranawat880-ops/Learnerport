package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LearnerBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainInformationModal(
  onClose: () -> Unit,
  onOpenReportContent: () -> Unit,
  modifier: Modifier = Modifier
) {
  val hindiNotice = """महत्वपूर्ण सूचना एवं अपलोड संबंधी जिम्मेदारी

इस एप्लिकेशन पर किसी भी पुस्तक, नोट्स, प्रश्नपत्र, PDF, फोटो या अन्य सामग्री को अपलोड करने से पहले यह सुनिश्चित करना आवश्यक है कि आपके पास उसे अपलोड और साझा करने का उचित अधिकार या अनुमति है।

यदि कोई उपयोगकर्ता किसी अन्य व्यक्ति, लेखक, प्रकाशक, संस्था या संगठन की पुस्तक, नोट्स, PDF अथवा किसी अन्य कॉपीराइट-संरक्षित सामग्री को संबंधित अधिकारधारी की अनुमति के बिना अपलोड या साझा करता है, तो इसके लिए संबंधित अपलोडर स्वयं जिम्मेदार होगा।

इसलिए किसी भी अन्य व्यक्ति की PDF, पुस्तक, नोट्स या अन्य संरक्षित सामग्री अपलोड करने से पहले संबंधित अधिकारधारी की अनुमति प्राप्त करना आवश्यक है, जब तक कि सामग्री को साझा करने की अनुमति किसी लागू लाइसेंस, कानून या अन्य वैध आधार के अंतर्गत पहले से उपलब्ध न हो।

केवल इंटरनेट पर उपलब्ध होने के कारण किसी सामग्री को स्वतंत्र रूप से अपलोड या वितरित करने का अधिकार प्राप्त नहीं हो जाता।

उपयोगकर्ताओं को केवल अपनी बनाई हुई सामग्री, अधिकृत सामग्री, उचित अनुमति से प्राप्त सामग्री, या वैध रूप से साझा की जा सकने वाली सामग्री ही प्रकाशित करनी चाहिए।

यदि किसी सामग्री के अधिकार या अनुमति के संबंध में संदेह हो, तो उसे अपलोड करने से पहले संबंधित अधिकारधारी से पुष्टि करें।

कॉपीराइट शिकायत, सामग्री हटाने, रिपोर्ट करने और संबंधित नीतियों के लिए एप्लिकेशन में उपलब्ध सहायता एवं रिपोर्टिंग विकल्पों का उपयोग करें।

— By Satish Ranawat"""

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Main Information", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        },
        actions = {
          IconButton(onClick = onOpenReportContent) {
            Icon(Icons.Default.Flag, contentDescription = "Report Infringement", tint = MaterialTheme.colorScheme.error)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = LearnerBlue.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Gavel, contentDescription = null, tint = LearnerBlue, modifier = Modifier.size(28.dp))
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Upload Permissions & Responsibility",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = LearnerBlue
            )
            Text(
              text = "Official Educational Policy & Content Guidelines",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = hindiNotice,
            fontSize = 14.sp,
            lineHeight = 24.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Normal
          )
        }
      }

      OutlinedButton(
        onClick = onOpenReportContent,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth().testTag("report_infringement_button")
      ) {
        Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Report Suspected Infringement / Content Issue")
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
