<#ftl output_format="HTML">
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="description" content="${setting.description!""}">
<meta name="keywords" content="${setting.keywords!""}">
<link rel="icon" type="image/x-icon" href="${setting.icon!""}">
<script src="https://cdn.tailwindcss.com?plugins=forms,typography"></script>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">
<script>
tailwind.config = {
  theme: {
    extend: {
      colors: {
        primary: '${setting.primaryColor!"#3B82F6"}',
      }
    }
  }
}
</script>
<style>
  :root {
    --primary: ${setting.primaryColor!"#3B82F6"};
  }
  body { font-family: 'Inter', system-ui, sans-serif; }
</style>
