<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en">
<head>
  <#include "/layouts/head.ftl">
  <title>${setting.name!"KotlinAdmin"} - ${pageTitle!"Auth"}</title>
</head>
<body class="bg-gray-100 min-h-screen flex items-center justify-center p-4">
  <#nested>
  <#include "/layouts/foot.ftl">
</body>
</html>
