<html>
<body style="font-family: Arial, sans-serif; color: #222;">
  <p>赛题 <strong>${taskName!""}</strong> 的第 ${submissionVersion!""} 版提交已通过。</p>
  <#if reviewerName?? && reviewerName?has_content>
  <p>审核人：${reviewerName}</p>
  </#if>
  <#if note?? && note?has_content>
  <p>${note}</p>
  <#elseif comment?? && comment?has_content>
  <p>反馈：${comment}</p>
  </#if>
</body>
</html>
