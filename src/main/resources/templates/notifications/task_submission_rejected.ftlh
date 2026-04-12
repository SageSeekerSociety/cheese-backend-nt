<html>
<body style="font-family: Arial, sans-serif; color: #222;">
  <p>Submission version ${submissionVersion!""} for task <strong>${taskName!""}</strong> was rejected.</p>
  <#if reviewerName?? && reviewerName?has_content>
  <p>Reviewer: ${reviewerName}</p>
  </#if>
  <#if note?? && note?has_content>
  <p>${note}</p>
  <#elseif comment?? && comment?has_content>
  <p>Feedback: ${comment}</p>
  </#if>
</body>
</html>
