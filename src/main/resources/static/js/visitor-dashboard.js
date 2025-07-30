// js/visitor-dashboard.js

// 辅助函数：跳转到凭证详情页
// 这个函数现在被放置在全局作用域，可以被 HTML 中的 onclick 调用
function viewVisitorCredential(visitorId) {
    console.log("尝试跳转到访客凭证详情页，访客ID:", visitorId); // 调试信息
    window.location.href = `visitor-qr-detail.html?id=${visitorId}`;
}

// 辅助函数：用于显示更友好的状态名称
// 这个函数也被放置在全局作用域
function getDisplayStatus(status) {
    switch (status) {
        case 'PENDING': return '待审核';
        case 'APPROVED': return '已通过';
        case 'REJECTED': return '已拒绝';
        case 'UNUSED': return '未使用';
        case 'ENTERED': return '已进入';
        case 'COMPLETED': return '已完成'; 
        case 'EXPIRED': return '已过期'; 
        default: return status;
    }
}


document.addEventListener('DOMContentLoaded', async function() {
    const appointmentsList = document.getElementById('appointmentsList');
    const messageElement = document.getElementById('message');
    const welcomeUsername = document.getElementById('welcomeUsername');
    const logoutButton = document.getElementById('logoutButton');

    // 获取并显示用户名
    async function fetchAndDisplayUsername() {
        try {
            const response = await fetch('/api/public/visitor-accounts/current-user'); // 假设有此API
            if (response.ok) {
                const data = await response.json();
                welcomeUsername.textContent = data.username || '访客';
            } else {
                welcomeUsername.textContent = '访客'; // 默认值
                console.warn('未能获取用户名，可能未登录或API不存在。');
            }
        } catch (error) {
            welcomeUsername.textContent = '访客'; // 默认值
            console.error('获取用户名网络错误:', error);
        }
    }
    fetchAndDisplayUsername(); // 页面加载时立即尝试获取并显示用户名


    // 登出按钮事件
    if (logoutButton) {
        logoutButton.addEventListener('click', async function() {
            try {
                const response = await fetch('/api/public/visitor-accounts/logout', {
                    method: 'POST'
                });
                if (response.ok) {
                    alert('您已成功登出。');
                    window.location.href = 'index.html'; // 返回首页
                } else {
                    alert('登出失败，请重试。');
                }
            } catch (error) {
                console.error('登出网络错误:', error);
                alert('登出失败: 无法连接到服务器。');
            }
        });
    }

    async function fetchAppointments() {
        appointmentsList.innerHTML = '<p>加载中...</p>'; // 显示加载状态
        try {
            const response = await fetch('/api/public/visitors/my-appointments'); // 调用新的API
            if (response.ok) {
                const appointments = await response.json();
                
                appointmentsList.innerHTML = ''; // 清空现有内容
                if (appointments && appointments.length > 0) {
                    appointments.forEach(app => { // 使用 app 作为当前循环项
                        const itemDiv = document.createElement('div'); // 定义 itemDiv
                        itemDiv.className = 'appointment-item';

                        let itemContent = `
                            <p><strong>预约ID:</strong> ${app.id}</p>
                            <p><strong>姓名:</strong> ${app.name}</p>
                            <p><strong>手机号:</strong> ${app.phone || 'N/A'}</p> <p><strong>拜访日期:</strong> ${app.visitDate}</p>
                            <p><strong>拜访时间:</strong> ${app.visitTimeStart || ''} - ${app.visitTimeEnd || ''}</p>
                            <p><strong>目的:</strong> ${app.purpose}</p>
                            <p><strong>状态:</strong> <span class="status-${app.status}">${getDisplayStatus(app.status)}</span></p> ${app.comments ? `<p><strong>审批意见:</strong> ${app.comments}</p>` : ''}
                            ${app.entryTime ? `<p><strong>入场时间:</strong> ${app.entryTime}</p>` : ''}
                            ${app.exitTime ? `<p><strong>离场时间:</strong> ${app.exitTime}</p>` : ''}
                        `;
                        // 关键修正：使用 app.status 和 app.id
                        if (app.status === 'APPROVED') {
                            itemContent += `
                                <button class="view-credential-btn" onclick="viewVisitorCredential(${app.id})">查看凭证</button>
                            `;
                        }

                        // 将构建好的所有 HTML 内容一次性设置给 itemDiv
                        itemDiv.innerHTML = itemContent;

                        // 将完整的 itemDiv 添加到页面的列表中
                        appointmentsList.appendChild(itemDiv);
                    });

                } else {
                    appointmentsList.innerHTML = '<p>您还没有任何预约记录。</p>';
                }
            } else if (response.status === 401) {
                messageElement.textContent = '您尚未登录，请先登录。';
                messageElement.style.color = 'red';
                appointmentsList.innerHTML = '';
                // 未登录，自动跳转到登录页
                setTimeout(() => {
                    window.location.href = 'visitor-login.html';
                }, 1500);
            } else {
                messageElement.textContent = '获取预约列表失败。';
                messageElement.style.color = 'red';
            }
        } catch (error) {
            console.error('网络错误:', error);
            messageElement.textContent = '加载预约列表失败: 无法连接到服务器。';
            messageElement.style.color = 'red';
        }
    }

    // 页面加载时立即获取预约列表
    fetchAppointments();

});