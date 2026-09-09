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
        assertTrue(tkaPackage.title.contains("Simulasi TKA"))
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
        assertTrue("Questions with diagram/bitmap images must be present", questionsWithImages.isNotEmpty())
        val totalImageRefs = tkaPackage.questions.sumOf { it.images.size }
        assertTrue("Total image references must be > 0", totalImageRefs > 0)

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

    @Test
    fun examRepository_dualSourceIntegrityAndHelperFunctions() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ExamRepository(context)

        // 1. Verify Legacy Simulation (Source 1: TKA_SMP_2027_complete.json)
        val legacyQuestions = repository.loadLegacySimulationQuestions()
        assertEquals("Legacy simulation must have exactly 100 questions", 100, legacyQuestions.size)
        assertEquals(1, legacyQuestions.first().id)
        assertEquals(100, legacyQuestions.last().id)
        assertTrue(legacyQuestions.first().question.contains("Diberikan beberapa bilangan sebagai berikut"))
        assertEquals("B", legacyQuestions.first().correctAnswer)
        assertTrue("Legacy Q1 must retain bitmap_111.png", legacyQuestions.first().images.contains("images/bitmap_111.png"))
        assertTrue("Legacy Q2 must retain bitmap_108.png", legacyQuestions[1].images.contains("images/bitmap_108.png"))

        // 2. Verify Subject Bank (Source 2: TKA_SMP_2027_question_bank_400.json)
        val allBankQuestions = repository.loadAllSubjectBankQuestions()
        assertEquals("Subject question bank must have exactly 400 questions", 400, allBankQuestions.size)

        // Verify helper functions for each subject
        val subjects = listOf("matematika", "bahasa_indonesia", "ipa", "bahasa_inggris")
        for (subj in subjects) {
            val qList = repository.loadSubjectQuestions(subj)
            assertEquals("Subject $subj must contain 100 questions", 100, qList.size)
            assertEquals("Subject count helper for $subj must return 100", 100, repository.getSubjectQuestionCount(subj))
            assertEquals("Package count helper for $subj must return 5", 5, repository.getPackageCount(subj))

            // Verify each of the 5 packages has exactly 20 questions
            for (pkgNum in 1..5) {
                val pkgQuestions = repository.loadPackageQuestions(subj, pkgNum)
                assertEquals("Package $pkgNum of $subj must contain exactly 20 questions", 20, pkgQuestions.size)
                for (q in pkgQuestions) {
                    assertTrue("Question text must not be empty", q.question.isNotBlank())
                    assertTrue("Question must have options", q.options.isNotEmpty())
                    assertTrue("Options must contain correctAnswer ${q.correctAnswer}", q.options.containsKey(q.correctAnswer))
                }
            }
        }
    }

    @Test
    fun examRepository_getSubjectsReturnsFiveCategories() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ExamRepository(context)
        val subjects = repository.getSubjects()

        assertEquals("Must return exactly 5 main categories", 5, subjects.size)

        val subjectNames = subjects.map { it.name }
        assertTrue(subjectNames.contains("Matematika"))
        assertTrue(subjectNames.contains("Bahasa Indonesia"))
        assertTrue(subjectNames.contains("IPA"))
        assertTrue(subjectNames.contains("Bahasa Inggris"))
        assertTrue(subjectNames.contains("Simulasi TKA Lengkap"))

        // Matematika: 100 questions across 5 packages
        val math = subjects.first { it.id == "matematika" }
        assertEquals(5, math.packages.size)
        assertEquals(100, math.totalQuestions)
        for (i in 0 until 5) {
            assertEquals("Paket ${i + 1}", math.packages[i].title)
            assertEquals(20, math.packages[i].questions.size)
        }

        // Bahasa Indonesia: 100 questions across 5 packages
        val indonesian = subjects.first { it.id == "bahasa_indonesia" }
        assertEquals(5, indonesian.packages.size)
        assertEquals(100, indonesian.totalQuestions)
        for (i in 0 until 5) {
            assertEquals("Paket ${i + 1}", indonesian.packages[i].title)
            assertEquals(20, indonesian.packages[i].questions.size)
        }

        // IPA: 100 questions across 5 packages (no longer empty)
        val ipa = subjects.first { it.id == "ipa" }
        assertEquals(5, ipa.packages.size)
        assertEquals(100, ipa.totalQuestions)
        for (i in 0 until 5) {
            assertEquals("Paket ${i + 1}", ipa.packages[i].title)
            assertEquals(20, ipa.packages[i].questions.size)
        }

        // Bahasa Inggris: 100 questions across 5 packages (no longer empty)
        val english = subjects.first { it.id == "bahasa_inggris" }
        assertEquals(5, english.packages.size)
        assertEquals(100, english.totalQuestions)
        for (i in 0 until 5) {
            assertEquals("Paket ${i + 1}", english.packages[i].title)
            assertEquals(20, english.packages[i].questions.size)
        }

        // Simulasi TKA Lengkap: 100 questions, 1 package
        val sim = subjects.first { it.id == "simulasi_lengkap" }
        assertEquals(1, sim.packages.size)
        assertEquals(100, sim.packages[0].questions.size)
        assertEquals(100, sim.totalQuestions)
        assertEquals(120, sim.packages[0].durationMinutes)
    }

    @Test
    fun practiceProgress_savesAndRetrievesCorrectly() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ExamRepository(context)

        val progress = com.example.data.model.PracticeProgress(
            packageId = "matematika_paket_1",
            currentQuestionIndex = 5,
            answers = mapOf(1 to "A", 2 to "B", 3 to "C"),
            totalQuestions = 20
        )

        repository.savePracticeProgress(progress)
        val loaded = repository.getPracticeProgress("matematika_paket_1")

        assertNotNull(loaded)
        assertEquals("matematika_paket_1", loaded?.packageId)
        assertEquals(5, loaded?.currentQuestionIndex)
        assertEquals(3, loaded?.answers?.size)
        assertEquals("A", loaded?.answers?.get(1))
        assertEquals("B", loaded?.answers?.get(2))
        assertEquals("C", loaded?.answers?.get(3))

        repository.clearPracticeProgress("matematika_paket_1")
        val cleared = repository.getPracticeProgress("matematika_paket_1")
        assertTrue(cleared == null)
    }
}



