package com.home.bingokotlin


import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

class NumberButton @JvmOverloads constructor(context : Context,
                                             attributeSet: AttributeSet? = null,
                                             defStyleAttr:Int = 0) : AppCompatButton(context,attributeSet,defStyleAttr) {
    var number : Int = 0
    var picked : Boolean = false
    var pos : Int = 0

}