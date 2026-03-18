<html>
<body style="font-family: Arial, sans-serif; color: #222;">
  <p>Your application for task <strong>${taskName!""}</strong> was rejected.</p>
  <#if rejectReason?? && rejectReason?has_content>
  <p>Reason: ${rejectReason}</p>
  </#if>
</body>
</html>
