document.addEventListener('DOMContentLoaded', function() {
    const visitorLoginForm = document.getElementById('visitorLoginForm');
    const loginMessage = document.getElementById('loginMessage');

    if (visitorLoginForm) {
        visitorLoginForm.addEventListener('submit', async function(event) {
            event.preventDefault();

            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;
            const crypto = window.crypto || window.msCrypto;
            const encoder = new TextEncoder();
            const data = encoder.encode(password);
            // 将 'MD5' 改为 'SHA-256'
            const hashBuffer = await crypto.subtle.digest('SHA-256', data);
            const hashArray = Array.from(new Uint8Array(hashBuffer));
            const hashedPassword = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
            console.log("前端计算的哈希密码:", hashedPassword);
            try {
                const response = await fetch('/api/public/visitor-accounts/login', {
                    method: 'POST',
                    headers: {
                        "Content-Type": "application/json" // 这一行现在是headers的第一个也是唯一一个属性
                    },
                    body: JSON.stringify({ // body 属性现在与 method, headers 同级
                        username: username,
                        password: hashedPassword
                    })
                });
                

                if (response.ok) {
                    // 登录成功，跳转到访客个人中心页面
                    window.location.href = 'visitor-dashboard.html';
                } else {
                    const errorData = await response.json();
                    loginMessage.textContent = '登录失败: ' + (errorData.message || '用户名或密码错误');
                    loginMessage.style.color = 'red';
                }
            } catch (error) {
                console.error('网络错误:', error);
                loginMessage.textContent = '登录失败: 无法连接到服务器。';
                loginMessage.style.color = 'red';
            }
        });
    }
});