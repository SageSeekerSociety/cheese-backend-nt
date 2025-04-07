package org.rucca.cheese.notification.channel

import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rucca.cheese.notification.models.NotificationChannel
import org.rucca.cheese.notification.models.NotificationType
import org.rucca.cheese.notification.services.NotificationTemplateService
import org.rucca.cheese.user.services.UserService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value // Import @Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
class EmailNotificationChannelHandler(
    private val mailSender: JavaMailSender,
    private val templateService: NotificationTemplateService,
    private val userService: UserService,
    @Value("\${cheese.notification.email.default-from}") private val defaultFromAddress: String,
) : NotificationChannelHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun supportedChannel(): NotificationChannel = NotificationChannel.EMAIL

    override suspend fun sendNotification(
        recipientId: Long,
        type: NotificationType,
        payload: Map<String, Any>,
        recipientLocale: Locale,
        isAggregatedFinalization: Boolean,
        renderedTitle: String?,
        renderedBody: String?,
    ) {
        withContext(Dispatchers.IO) {
            try {
                val userEmail = userService.getUserEmail(recipientId)
                if (userEmail.isNullOrBlank()) {
                    log.warn(
                        "No email address found for recipient ID: {}. Skipping email notification.",
                        recipientId,
                    )
                    return@withContext
                }

                val subject =
                    renderedTitle
                        ?: templateService.renderTitle(
                            type,
                            NotificationChannel.EMAIL,
                            payload,
                            recipientLocale,
                        )
                val htmlBody =
                    renderedBody
                        ?: templateService.renderBody(
                            type,
                            NotificationChannel.EMAIL,
                            payload,
                            recipientLocale,
                        )

                val message = mailSender.createMimeMessage()
                val helper = MimeMessageHelper(message, true, "UTF-8")

                helper.setFrom(defaultFromAddress)
                helper.setTo(userEmail)
                helper.setSubject(subject)
                helper.setText(htmlBody, true)

                mailSender.send(message)
                log.info(
                    "Email notification (type: {}) sent successfully to recipient ID: {}",
                    type,
                    recipientId,
                )
            } catch (e: Exception) {
                log.error(
                    "Failed to send email notification for recipient ID {}: {}",
                    recipientId,
                    e.message,
                    e,
                )
            }
        }
    }
}
