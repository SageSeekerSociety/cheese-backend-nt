<html>
<body style="font-family: Arial, sans-serif; color: #222;">
  <p><strong>${participantName!""}</strong> applied for task <strong>${taskName!""}</strong>.</p>
  <#if applyReason?? && applyReason?has_content>
  <p>Reason: ${applyReason}</p>
  </#if>
</body>
</html>
