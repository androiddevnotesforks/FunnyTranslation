package com.funny.translation.translate.ui.widget

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.delay

private const val TAG = "AutoFadeInComposableCol"
@Composable
fun AutoFadeInComposableColumn(
    modifier: Modifier = Modifier,
    state: AutoFadeInColumnState = rememberAutoFadeInColumnState(),
    fadeInTime: Int = 1000,
    fadeOffsetY: Int = 100,
    content: @Composable FadeInColumnScope.() -> Unit
) {
    var whetherFadeIn: List<Boolean> = arrayListOf()
    
    val fadeInAnimatable = remember {
        Animatable(0f)
    }

    LaunchedEffect(state.currentFadeIndex){
        // 等待初始化完成
        while (whetherFadeIn.isEmpty()){ delay(50) }
        if (state.currentFadeIndex == -1) {
            // 找到第一个需要渐入的元素
            state.currentFadeIndex = whetherFadeIn.indexOf(true)
        }
        // 开始动画
        fadeInAnimatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = fadeInTime,
                easing = LinearEasing
            )
        )
        state.finishedFadeIndex = state.currentFadeIndex

        if(state.finishedFadeIndex >= whetherFadeIn.size - 1) return@LaunchedEffect
        for (i in state.finishedFadeIndex + 1 until whetherFadeIn.size){
            if (whetherFadeIn[i]){
                state.currentFadeIndex = i
                fadeInAnimatable.snapTo(0f)
                break
            }
        }
    }

    val measurePolicy = MeasurePolicy { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints.copy(minHeight = 0, minWidth = 0))
        }
        whetherFadeIn = placeables.map { placeable ->
            ((placeable.parentData as? FadeInColumnData) ?: FadeInColumnData()).fade
        }
        var y = 0
        layout(constraints.maxWidth, placeables.sumOf { it.height }) {
            placeables.forEachIndexed { index, placeable ->
                val actualY = if (state.currentFadeIndex == index) {
                    y + (( 1 - fadeInAnimatable.value) * fadeOffsetY).toInt()
                } else {
                    y
                }
                placeable.placeRelativeWithLayer(0, actualY){
                    alpha = if (!whetherFadeIn[index]) 1f else
                                if (index == state.currentFadeIndex) fadeInAnimatable.value else
                                    if (index <= state.finishedFadeIndex) 1f else 0f
                }
                y += placeable.height
            }.also {
                y = 0
            }
        }
    }
    Layout(modifier = modifier, content = { FadeInColumnScopeInstance.content() }, measurePolicy = measurePolicy)
}

class FadeInColumnData(val fade: Boolean = true) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any =
        this@FadeInColumnData
}

interface FadeInColumnScope {
    @Stable
    fun Modifier.fadeIn(whetherFadeIn: Boolean = true): Modifier
}

object FadeInColumnScopeInstance : FadeInColumnScope {
    override fun Modifier.fadeIn(whetherFadeIn: Boolean): Modifier = this.then(FadeInColumnData(whetherFadeIn))
}

class AutoFadeInColumnState {
    var currentFadeIndex by mutableStateOf(-1)
    var finishedFadeIndex by mutableStateOf(0)
    
    companion object {
        val Saver = listSaver<AutoFadeInColumnState, Int>(
            save = { listOf(it.currentFadeIndex, it.finishedFadeIndex) },
            restore = {
                AutoFadeInColumnState().apply {
                    currentFadeIndex = it[0]; finishedFadeIndex = it[1]
                }
            }
        )
    }
}

@Composable
fun rememberAutoFadeInColumnState(): AutoFadeInColumnState {
    return rememberSaveable(saver = AutoFadeInColumnState.Saver) { AutoFadeInColumnState() }
}