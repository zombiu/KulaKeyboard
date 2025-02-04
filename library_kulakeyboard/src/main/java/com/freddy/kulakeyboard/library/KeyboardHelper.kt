package com.freddy.kulakeyboard.library

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView

/**
 * @author  FreddyChen
 * @name
 * @date    2020/06/08 11:49
 * @email   chenshichao@outlook.com
 * @github  https://github.com/FreddyChen
 * @desc
 */
class KeyboardHelper {

    private lateinit var context: Context
    private var rootLayout: ViewGroup? = null
    private var recyclerView: RecyclerView? = null
    private var inputPanel: IInputPanel? = null
    private var expressionPanel: IPanel? = null
    private var morePanel: IPanel? = null
    private var keyboardStatePopupWindow: KeyboardStatePopupWindow? = null
    private var scrollBodyLayout: Boolean = false

    companion object {
        var keyboardHeight = 0
        var inputPanelHeight = 0
        var expressionPanelHeight = 0
        var morePanelHeight = 0
    }

    fun init(context: Context): KeyboardHelper {
        this.context = context
        return this
    }

    fun reset() {
        inputPanel?.reset()
        expressionPanel?.reset()
        morePanel?.reset()
    }

    fun release() {
        reset()
        inputPanel = null
        expressionPanel = null
        morePanel = null
        keyboardStatePopupWindow?.dismiss()
        keyboardStatePopupWindow = null
    }

    fun setKeyboardHeight(keyboardHeight: Int): KeyboardHelper {
        KeyboardHelper.keyboardHeight = keyboardHeight
        if (inputPanelHeight == 0) {
            inputPanelHeight = keyboardHeight
        }
        return this
    }

    fun bindRootLayout(rootLayout: ViewGroup): KeyboardHelper {
        this.rootLayout = rootLayout
        keyboardStatePopupWindow = KeyboardStatePopupWindow(context, rootLayout)
        keyboardStatePopupWindow?.setOnKeyboardStateListener(object :
            KeyboardStatePopupWindow.OnKeyboardStateListener {
            override fun onOpened(keyboardHeight: Int) {
                KeyboardHelper.keyboardHeight = keyboardHeight
                inputPanel?.onSoftKeyboardOpened()
                onKeyboardStateListener?.onOpened(keyboardHeight)
                inputPanel?.apply {
                    inputPanelHeight = getPanelHeight()
                }
                expressionPanel?.apply {
                    expressionPanelHeight = getPanelHeight()
                }
                morePanel?.apply {
                    morePanelHeight = getPanelHeight()
                }
            }

            override fun onClosed() {
                inputPanel?.onSoftKeyboardClosed()
                onKeyboardStateListener?.onClosed()
            }
        })
        return this
    }

    fun bindRecyclerView(recyclerView: RecyclerView): KeyboardHelper {
        this.recyclerView = recyclerView
        return this
    }

    fun <P : IPanel> bindVoicePanel(panel: P): KeyboardHelper {
        return this
    }

    fun <P : IInputPanel> bindInputPanel(panel: P): KeyboardHelper {
        this.inputPanel = panel
        inputPanelHeight = panel.getPanelHeight()
        panel.setOnInputStateChangedListener(object : OnInputPanelStateChangedListener {
            override fun onShowVoicePanel() {
                if (expressionPanel !is ViewGroup || morePanel !is ViewGroup) return
                expressionPanel?.let {
                    it as ViewGroup
                    it.visibility = View.GONE
                }
                morePanel?.let {
                    it as ViewGroup
                    it.visibility = View.GONE
                }
            }

            override fun onShowInputMethodPanel() {
                if (expressionPanel !is ViewGroup || morePanel !is ViewGroup) return
                expressionPanel?.let {
                    it as ViewGroup
                    it.visibility = View.GONE
                }
                morePanel?.let {
                    it as ViewGroup
                    it.visibility = View.GONE
                }
            }

            override fun onShowExpressionPanel() {
                if (expressionPanel !is ViewGroup) return
                expressionPanel?.let {
                    it as ViewGroup
                    it.visibility = View.VISIBLE
                }
            }

            override fun onShowMorePanel() {
                if (morePanel !is ViewGroup) return
                morePanel?.let {
                    it as ViewGroup
                    it.visibility = View.VISIBLE
                }
            }
        })
        panel.setOnLayoutAnimatorHandleListener { panelType, lastPanelType, fromValue, toValue ->
            // 动画触发流程：点击输入框或者表情按钮-> 输入面板接收到触摸事件 -> 回调给bindInputPanel
            handlePanelMoveAnimator(panelType, lastPanelType, fromValue, toValue)
        }
        return this
    }

    fun <P : IPanel> bindExpressionPanel(panel: P): KeyboardHelper {
        this.expressionPanel = panel
        expressionPanelHeight = panel.getPanelHeight()
        return this
    }

    fun <P : IPanel> bindMorePanel(panel: P): KeyboardHelper {
        this.morePanel = panel
        morePanelHeight = panel.getPanelHeight()
        return this
    }

    fun setScrollBodyLayout(scrollBodyLayout: Boolean): KeyboardHelper {
        this.scrollBodyLayout = scrollBodyLayout
        return this
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun handlePanelMoveAnimator(
        panelType: PanelType,
        lastPanelType: PanelType,
        fromValue: Float,
        toValue: Float
    ) {
        Log.d("KulaKeyboardHelper", "panelType = $panelType, lastPanelType = $lastPanelType")
        // 创建recylcerView平移动画
        val recyclerViewTranslationYAnimator: ObjectAnimator =
            ObjectAnimator.ofFloat(recyclerView, "translationY", fromValue, toValue)
        // 创建 输入面板 平移动画
        val inputPanelTranslationYAnimator: ObjectAnimator =
            ObjectAnimator.ofFloat(inputPanel, "translationY", fromValue, toValue)
        var panelTranslationYAnimator: ObjectAnimator? = null
        when (panelType) {
            PanelType.INPUT_MOTHOD -> {
                expressionPanel?.reset()
                morePanel?.reset()
            }
            PanelType.VOICE -> {
                expressionPanel?.reset()
                morePanel?.reset()
            }
            PanelType.EXPRESSION -> {
                morePanel?.reset()
                panelTranslationYAnimator =
                    ObjectAnimator.ofFloat(expressionPanel, "translationY", fromValue, toValue)
            }
            PanelType.MORE -> {
                expressionPanel?.reset()
                panelTranslationYAnimator =
                    ObjectAnimator.ofFloat(morePanel, "translationY", fromValue, toValue)
            }
            else -> {
            }
        }
        val animatorSet = AnimatorSet()
        animatorSet.duration = 250
        animatorSet.interpolator = DecelerateInterpolator()
        if (panelTranslationYAnimator == null) {
            if (scrollBodyLayout) {
                // 播放 底部输入面板 和 recyclerView 平移动画
                animatorSet.play(inputPanelTranslationYAnimator)
                    .with(recyclerViewTranslationYAnimator)
            } else {
                animatorSet.play(inputPanelTranslationYAnimator)
            }
        } else {
            if (scrollBodyLayout) {
                animatorSet.play(inputPanelTranslationYAnimator)
                    .with(recyclerViewTranslationYAnimator).with(panelTranslationYAnimator)
            } else {
                animatorSet.play(inputPanelTranslationYAnimator).with(panelTranslationYAnimator)
            }
        }
        animatorSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                recyclerView?.requestLayout()
                expressionPanel?.let {
                    it as ViewGroup
                    it.requestLayout()
                }
                morePanel?.let {
                    it as ViewGroup
                    it.requestLayout()
                }
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
        animatorSet.start()
    }

    private var onKeyboardStateListener: OnKeyboardStateListener? = null
    fun setOnKeyboardStateListener(listener: OnKeyboardStateListener?): KeyboardHelper {
        this.onKeyboardStateListener = listener
        return this
    }

    interface OnKeyboardStateListener {
        fun onOpened(keyboardHeight: Int)
        fun onClosed()
    }
}