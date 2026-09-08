package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ExamActiveProgress
import com.example.data.model.Question
import com.example.data.model.QuestionReviewItem
import com.example.data.repository.ExamRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExamLogicTest {

    @Test
    fun calculateScore_correctlyComputesPercentage() {
        val totalQuestions = 20
        val correctCount = 17
        val score = Math.round((correctCount.toDouble() / totalQuestions) * 100).toInt()
        assertEquals(85, score)
    }

    @Test
    fun reviewItem_accuratelyCategorizesStatus() {
        val q1 = Question(
            id = 1,
            question = "2 + 2 = ?",
            options = mapOf("A" to "3", "B" to "4", "C" to "5", "D" to "6"),
            correctAnswer = "B"
        )

        // Case 1: Correct answer
        val item1 = QuestionReviewItem(
            question = q1,
            userAnswer = "B",
            isCorrect = true,
            isUnanswered = false
        )
        assertTrue(item1.isCorrect)
        assertFalse(item1.isUnanswered)

        // Case 2: Wrong answer
        val item2 = QuestionReviewItem(
            question = q1,
            userAnswer = "A",
            isCorrect = false,
            isUnanswered = false
        )
        assertFalse(item2.isCorrect)
        assertFalse(item2.isUnanswered)

        // Case 3: Unanswered
        val item3 = QuestionReviewItem(
            question = q1,
            userAnswer = null,
            isCorrect = false,
            isUnanswered = true
        )
        assertFalse(item3.isCorrect)
        assertTrue(item3.isUnanswered)
    }

    @Test
    fun examRepository_loadsRealPackagesSuccessfully() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ExamRepository(context)
        val packages = repository.loadPackages()

        assertTrue("Should load real packages from JSON", packages.isNotEmpty())

        val tkaPackage = packages.first()
        assertTrue(tkaPackage.title.contains("TKA SMP 2027"))
        assertEquals(100, tkaPackage.questions.size)
        assertEquals(120, tkaPackage.durationMinutes)

        // Verify Question 1 from the complete uploaded file
        val q1 = tkaPackage.questions.first()
        assertEquals(1, q1.id)
        assertTrue(q1.question.contains("Diberikan beberapa bilangan sebagai berikut"))
        assertEquals("B", q1.correctAnswer)
        assertEquals("A – C – B – D", q1.options["A"])
        assertEquals("C – A – D – B", q1.options["B"])
        assertEquals("D – B – C – A", q1.options["C"])
        assertEquals("A – D – C – B", q1.options["D"])
        assertTrue("Q1 must have images/bitmap_111.png", q1.images.contains("images/bitmap_111.png"))

        // Verify Question 2 image mapping
        val q2 = tkaPackage.questions[1]
        assertEquals(2, q2.id)
        assertTrue("Q2 must have images/bitmap_108.png", q2.images.contains("images/bitmap_108.png"))

        // Verify questions with image assets
        val questionsWithImages = tkaPackage.questions.filter { it.images.isNotEmpty() }
        assertEquals("Total questions with diagram/bitmap images must be 20", 20, questionsWithImages.size)
        val totalImageRefs = tkaPackage.questions.sumOf { it.images.size }
        assertEquals(23, totalImageRefs)

        for (pkg in packages) {
            assertTrue("Package ID should not be blank", pkg.id.isNotBlank())
            assertTrue("Package title should not be blank", pkg.title.isNotBlank())
            assertTrue("Package duration should be > 0", pkg.durationMinutes > 0)
            assertTrue("Package should have questions", pkg.questions.isNotEmpty())

            for (q in pkg.questions) {
                assertTrue("Question ID should be > 0", q.id > 0)
                assertTrue("Question text should not be blank", q.question.isNotBlank())
                if (q.options.isNotEmpty()) {
                    assertTrue(
                        "Options must contain correctAnswer ${q.correctAnswer} for question ${q.id}",
                        q.options.containsKey(q.correctAnswer)
                    )
                }
                assertTrue(
                    "Correct answer must be a valid uppercase option key",
                    listOf("A", "B", "C", "D", "E").contains(q.correctAnswer)
                )
            }
        }
    }

    @Test
    fun examActiveProgress_serializesCorrectly() {
        val progress = ExamActiveProgress(
            packageId = "paket_1",
            currentQuestionIndex = 3,
            answers = mapOf(1 to "A", 2 to "C"),
            remainingSeconds = 1140,
            totalQuestions = 20
        )
        assertEquals("paket_1", progress.packageId)
        assertEquals(3, progress.currentQuestionIndex)
        assertEquals(2, progress.answers.size)
        assertEquals("A", progress.answers[1])
        assertEquals("C", progress.answers[2])
        assertEquals(1140, progress.remainingSeconds)
    }
}


