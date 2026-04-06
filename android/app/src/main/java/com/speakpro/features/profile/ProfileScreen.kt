package com.speakpro.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpBodyMedium
import com.speakpro.designsystem.theme.SpError
import com.speakpro.designsystem.theme.SpTextPrimary
import com.speakpro.designsystem.theme.SpTextSecondary
import com.speakpro.designsystem.theme.SpTitleLarge
import com.speakpro.designsystem.theme.SpWhite

/**
 * 个人中心页面
 */
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "我的",
            style = SpTitleLarge,
            color = SpTextPrimary,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── 头像区域 ──
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SpAccent.copy(alpha = 0.1f)),
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "头像",
                tint = SpAccent,
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = userName,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = SpTextPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = userEmail,
            style = SpBodyMedium,
            color = SpTextSecondary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (userRole == "teacher") "教师" else "学生",
            style = SpBodyMedium,
            color = SpAccent,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── 信息卡片 ──
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SpWhite),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ProfileRow(label = "用户名", value = userName)
                HorizontalDivider(
                    color = SpTextSecondary.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                ProfileRow(label = "邮箱", value = userEmail)
                HorizontalDivider(
                    color = SpTextSecondary.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                ProfileRow(
                    label = "角色",
                    value = if (userRole == "teacher") "教师" else "学生",
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── 退出登录 ──
        Button(
            onClick = onLogout,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SpError.copy(alpha = 0.1f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            Text(
                text = "退出登录",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SpError,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = SpBodyMedium,
            color = SpTextSecondary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = SpBodyMedium,
            fontWeight = FontWeight.Medium,
            color = SpTextPrimary,
        )
    }
}
