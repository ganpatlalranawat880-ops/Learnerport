package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommentItem
import com.example.data.model.ThoughtItem
import com.example.ui.theme.LearnerBlue
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThoughtCommentsModal(
  thought: ThoughtItem,
  commentsFlow: Flow<List<CommentItem>>,
  onClose: () -> Unit,
  onAddComment: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val comments by commentsFlow.collectAsState(initial = emptyList())
  var commentInput by remember { mutableStateOf("") }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Thought Discussion", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    },
    bottomBar = {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = commentInput,
            onValueChange = { commentInput = it },
            placeholder = { Text("Add to discussion...", fontSize = 13.sp) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          )

          Button(
            onClick = {
              if (commentInput.isNotBlank()) {
                onAddComment(commentInput)
                commentInput = ""
              }
            },
            enabled = commentInput.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LearnerBlue)
          ) {
            Text("Reply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(4.dp))
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "@${thought.authorUsername} wrote:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LearnerBlue)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = thought.content, fontSize = 13.sp, lineHeight = 18.sp)
          }
        }
      }

      item {
        Text("Comments (${comments.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
      }

      if (comments.isEmpty()) {
        item {
          Text("No replies yet. Share your thoughts on this educational topic!", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      } else {
        items(comments) { c ->
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(text = c.userDisplayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = c.formattedDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(text = c.commentText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}
