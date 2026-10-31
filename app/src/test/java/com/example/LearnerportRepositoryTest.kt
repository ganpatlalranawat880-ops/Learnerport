package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ResourceType
import com.example.data.repository.LearnerportRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LearnerportRepositoryTest {

  private val repository = LearnerportRepository

  @Before
  fun setUp() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    repository.init(context)

    // Ensure at least one resource is available for testing
    val current = repository.getAllResources().first()
    if (current.isEmpty()) {
      repository.publishResource(
        title = "Introductory Computer Networks",
        description = "OSI layer overview and TCP/IP protocol stack",
        resourceType = ResourceType.NOTES,
        subject = "Computer Science",
        topic = "Networking",
        educationalLevel = "Undergraduate Degree",
        programmingLanguage = null,
        language = "English",
        tags = listOf("Networks", "TCP")
      )
    }
  }

  @Test
  fun testUserDataLoaded() = runBlocking {
    val resources = repository.getAllResources().first()
    assertTrue(resources.isNotEmpty())

    val user = repository.getCurrentUser().first()
    assertTrue(user.isCurrentUser)
  }

  @Test
  fun testSaveAndDownloadToggles() = runBlocking {
    val resources = repository.getAllResources().first()
    val testItem = resources.first()

    val wasSaved = testItem.isSaved
    repository.toggleSaveResource(testItem.id)
    val updatedItem = repository.getResourceById(testItem.id)
    assertNotNull(updatedItem)
    assertEquals(!wasSaved, updatedItem?.isSaved)

    // Test download recording
    val initialDownloads = updatedItem?.downloadsCount ?: 0
    repository.recordDownload(testItem.id)
    val downloadedItem = repository.getResourceById(testItem.id)
    assertEquals(initialDownloads + 1, downloadedItem?.downloadsCount)
    assertTrue(downloadedItem?.isDownloaded == true)
  }

  @Test
  fun testRatingCalculation() = runBlocking {
    val resources = repository.getAllResources().first()
    val testItem = resources.first()

    repository.submitRating(testItem.id, 5)
    val rated = repository.getResourceById(testItem.id)
    assertNotNull(rated)
    assertEquals(5, rated?.userRating)
    assertTrue((rated?.averageRating ?: 0f) > 0f)
  }

  @Test
  fun testProfileUpdate() = runBlocking {
    val uniqueName = "learner_alex"
    val success = repository.updateCurrentUserProfile(
      displayName = "Alex Morgan",
      username = uniqueName,
      bio = "CS Enthusiast",
      educationalLevel = "Undergraduate Degree",
      courseOrDegree = "B.Tech. Computer Science",
      institution = "IIT Bombay",
      avatarUrl = ""
    )
    assertTrue("Profile update should succeed", success)
    val user = repository.getCurrentUser().first()
    assertEquals(uniqueName, user.username)
    assertEquals("Alex Morgan", user.displayName)
  }

  @Test
  fun testPublishNewResource() = runBlocking {
    val initialCount = repository.getAllResources().first().size
    repository.publishResource(
      title = "New Distributed Systems Notes",
      description = "Consensus algorithms, Raft, Paxos",
      resourceType = ResourceType.NOTES,
      subject = "Computer Science",
      topic = "Distributed Systems",
      educationalLevel = "College",
      programmingLanguage = "Go",
      language = "English",
      tags = listOf("DistributedSystems", "Raft")
    )

    val updatedResources = repository.getAllResources().first()
    assertEquals(initialCount + 1, updatedResources.size)
    assertEquals("New Distributed Systems Notes", updatedResources.first().title)
  }

  @Test
  fun testPdfHeaderValidation() {
    val validPdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34) // %PDF-1.4
    assertTrue(com.example.data.util.ImageCompressor.isValidPdfBytes(validPdfBytes))

    val invalidPdfBytes = "Plain text document".toByteArray()
    assertFalse(com.example.data.util.ImageCompressor.isValidPdfBytes(invalidPdfBytes))

    assertFalse(com.example.data.util.ImageCompressor.isValidPdfBytes(byteArrayOf()))
    assertFalse(com.example.data.util.ImageCompressor.isValidPdfBytes(null))
  }

  @Test
  fun testUploadPdfValidationRejectsCorruptedFile() = runBlocking {
    val invalidBytes = "This is not a real PDF file".toByteArray()
    val result = repository.uploadAndPublishResource(
      title = "Corrupted File Test",
      description = "Should fail",
      resourceType = ResourceType.NOTES,
      subject = "Math",
      topic = "Calculus",
      educationalLevel = "College",
      programmingLanguage = null,
      language = "English",
      tags = emptyList(),
      pdfBytes = invalidBytes,
      pdfFileName = "bad.pdf",
      coverBytes = null,
      fileSize = "1 KB"
    )

    assertTrue("Should fail on corrupted PDF header", result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
  }

  @Test
  fun testAuthVerificationRules() {
    // Unauthenticated or null user must never be treated as verified
    assertFalse(com.example.data.remote.FirebaseManager.isUserVerified(null))
  }
}
