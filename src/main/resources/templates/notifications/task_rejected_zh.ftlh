<html>
<body style="font-family: Arial, sans-serif; color: #222;">
  <p>你的赛题 <strong>${taskName!""}</strong> 已被驳回。</p>
  <#if rejectReason?? && rejectReason?has_content>
  <p>原因：${rejectReason}</p>
  </#if>
</body>
</html>
