<html>
<body style="font-family: Arial, sans-serif; color: #222;">
  <p><strong>${participantName!""}</strong> 报名了赛题 <strong>${taskName!""}</strong>。</p>
  <#if applyReason?? && applyReason?has_content>
  <p>申请说明：${applyReason}</p>
  </#if>
</body>
</html>
