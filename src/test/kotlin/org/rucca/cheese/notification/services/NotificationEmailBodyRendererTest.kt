package org.rucca.cheese.notification.services

import freemarker.template.Configuration
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.rucca.cheese.notification.models.NotificationType

class NotificationEmailBodyRendererTest {
    private val renderer = NotificationEmailBodyRenderer(freeMarkerConfiguration())

    @Test
    fun `render should fall back to default template for locale variants`() {
        val html =
            renderer.render(
                NotificationType.MENTION,
                mapOf("actorName" to "Alice", "discussionTitle" to "Roadmap"),
                Locale.CHINA,
            )

        assertThat(html).contains("Alice")
        assertThat(html).contains("Roadmap")
        assertThat(html).contains("mentioned you in discussion")
    }

    @Test
    fun `render should support task submission approved templates`() {
        val html =
            renderer.render(
                NotificationType.TASK_SUBMISSION_APPROVED,
                mapOf(
                    "taskName" to "API Review",
                    "reviewerName" to "Bob",
                    "submissionVersion" to 3,
                    "score" to 95,
                ),
                Locale.ENGLISH,
            )

        assertThat(html).contains("API Review")
        assertThat(html).contains("Bob")
        assertThat(html).contains("Score: 95")
    }

    private fun freeMarkerConfiguration(): Configuration {
        return Configuration(Configuration.VERSION_2_3_34).apply {
            defaultEncoding = "UTF-8"
            setClassLoaderForTemplateLoading(javaClass.classLoader, "templates")
        }
    }
}
