package com.funny.translation.translate.ui.thanks

import android.content.ComponentName
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.W800
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funny.translation.translate.R
import com.funny.translation.translate.WebViewActivity
import com.funny.translation.translate.ui.screen.NavigationIcon

@Composable
fun ThanksScreen() {
    val vm : ThanksViewModel = viewModel()
    val sponsors by vm.sponsors.observeAsState()
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ){
        Row(
            Modifier
                .fillMaxWidth()
                .padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(id = R.string.thanks), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        }
        SponsorList(sponsors = sponsors!!)
//        Spacer(modifier = Modifier.height(4.dp))
        Text(modifier = Modifier.fillMaxWidth(),text = "上述排名仅以时间为序，不分先后\n您的支持让应用变得更好\n赞助完全自愿，且短期内无较明显回报，请量力而为", textAlign = TextAlign.Center, fontSize = 12.sp, color = LocalContentColor.current.copy(0.5f))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(id = R.string.join_sponsor), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier
            .height(80.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colors.surface),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ){
            SponsorIcon(
                load_url = "https://afdian.net/@funnysaltyfish?tab=home",
                resourceId = R.drawable.ic_aifadian,
                contentDes = "爱发电"
            )
            SponsorIcon(
                load_url = "https://gitee.com/funnysaltyfish/blog-drawing-bed/raw/master/img/202111072055394.png",
                resourceId = R.drawable.ic_wechat,
                contentDes = "微信"
            )
        }
    }
}

@Composable
fun SponsorList(
    sponsors : List<Sponsor>,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        itemsIndexed(sponsors) { _: Int, item: Sponsor ->
            SponsorItem(sponsor = item)
        }
    }
}

@Composable
fun SponsorIcon(
    load_url : String,
    resourceId : Int,
    contentDes : String
) {
    val context = LocalContext.current
    IconButton(onClick = {
        val intent = Intent(context, WebViewActivity::class.java)
        intent.putExtra("load_url",load_url)
        context.startActivity(intent)
    }) {
        Icon(
            modifier = Modifier.size(48.dp) ,
            painter = painterResource(id = resourceId),
            contentDescription = contentDes,
            tint = Color.Unspecified
        )
    }
}

@Composable
fun SponsorItem(
    sponsor : Sponsor,
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colors.surface)
        .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
        .animateContentSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ){
        Column(
            modifier = Modifier
        ){
            Text(sponsor.name, fontSize = 18.sp, fontWeight = FontWeight.W600)
            sponsor.message?.let {
                Text(
                    it,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 2.dp),
                    color = LocalContentColor.current.copy(0.7f)
                )
            }
        }
        Column(
            //modifier = Modifier.weight(1f)
        ) {
            Text(text = sponsor.money.moneyText, Modifier.padding(end = 4.dp), fontWeight = W800, fontSize = 28.sp)
        }
    }
}

private val Int.moneyText : String
    get() = String.format("%.2f元", this / 100f)