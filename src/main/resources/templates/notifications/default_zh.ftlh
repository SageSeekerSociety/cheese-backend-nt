<html>
<body style="font-family: Arial, sans-serif; color: #222;">
  <h3>${titleText!heading!""}</h3>
  <p>${bodyText!summary!""}</p>
  <#if note?? && note?has_content>
  <p>${note}</p>
  </#if>
</body>
</html>
