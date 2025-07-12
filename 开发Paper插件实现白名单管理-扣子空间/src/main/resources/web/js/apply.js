document.addEventListener('DOMContentLoaded', function() {
    // Version selection handling
    const versionRadios = document.querySelectorAll('input[name="version"]');
    const bedrockIdGroup = document.getElementById('bedrockIdGroup');
    
    function toggleBedrockIdField() {
        const selectedVersion = document.querySelector('input[name="version"]:checked').value;
        if (selectedVersion === 'BOTH') {
            bedrockIdGroup.style.display = 'block';
        } else {
            bedrockIdGroup.style.display = 'none';
            // Clear bedrock ID when not needed
            document.getElementById('bedrockId').value = '';
        }
        
        // Update main ID label based on selected version
        const gameIdLabel = document.querySelector('label[for="gameId"]');
        if (selectedVersion === 'BEDROCK') {
            gameIdLabel.textContent = '手机版ID';
        } else {
            gameIdLabel.textContent = '游戏ID (Java版)';
        }
    }
    
    // Initial toggle
    toggleBedrockIdField();
    
    // Add event listeners to version radios
    versionRadios.forEach(radio => {
        radio.addEventListener('change', toggleBedrockIdField);
    });
    
    // Form submission handling
    const applicationForm = document.getElementById('applicationForm');
    if (applicationForm) {
        applicationForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const formData = new FormData(this);
            const submitButton = this.querySelector('button[type="submit"]');
            const originalButtonText = submitButton.innerHTML;
            
            try {
                // Show loading state
                submitButton.disabled = true;
                submitButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 提交中...';
                
                const response = await fetch('/apply', {
                    method: 'POST',
                    body: formData
                });
                
                if (response.ok) {
                    // Show success message
                    document.getElementById('formContainer').innerHTML = `
                        <div class="alert alert-success">
                            <h4 class="alert-heading">申请提交成功！</h4>
                            <p>感谢您的申请，管理员将尽快处理。请耐心等待审批结果。</p>
                            <hr>
                            <p class="mb-0"><a href="/" class="btn btn-primary">返回首页</a></p>
                        </div>
                    `;
                } else {
                    // Show error message
                    const errorText = await response.text();
                    throw new Error(errorText || '提交申请时发生错误，请稍后重试');
                }
            } catch (error) {
                // Show error alert
                const errorAlert = document.createElement('div');
                errorAlert.className = 'alert alert-danger';
                errorAlert.textContent = error.message;
                
                const formContainer = document.getElementById('formContainer');
                formContainer.insertBefore(errorAlert, formContainer.firstChild);
                
                // Remove error after 5 seconds
                setTimeout(() => {
                    errorAlert.remove();
                }, 5000);
            } finally {
                // Restore button state
                submitButton.disabled = false;
                submitButton.innerHTML = originalButtonText;
            }
        });
    }
    
    // Check for success parameter in URL
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('success')) {
        const successAlert = document.createElement('div');
        successAlert.className = 'alert alert-success';
        successAlert.innerHTML = `
            <h4 class="alert-heading">申请提交成功！</h4>
            <p>感谢您的申请，管理员将尽快处理。请耐心等待审批结果。</p>
        `;
        
        const container = document.querySelector('.container');
        container.insertBefore(successAlert, container.firstChild);
        
        // Remove alert after 5 seconds
        setTimeout(() => {
            successAlert.remove();
            // Update URL to remove success parameter
            history.replaceState({}, document.title, window.location.pathname);
        }, 5000);
    }
});