<html>
<body style="font-family: Arial, sans-serif; color: #222;">
  <p>你报名赛题 <strong>${taskName!""}</strong> 的申请已被驳回。</p>
  <#if rejectReason?? && rejectReason?has_content>
  <p>原因：${rejectReason}</p>
  </#if>
</body>
</html>
