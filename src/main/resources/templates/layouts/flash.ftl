<#-- Inline flash alert — included inside the page body when flash is present -->
<div class="alert alert-${flash.key} mb-4">
  <i class="fas fa-<#if flash.key == 'success'>check-circle<#else>exclamation-circle</#if> me-1"></i>
  ${flash.message}
</div>
