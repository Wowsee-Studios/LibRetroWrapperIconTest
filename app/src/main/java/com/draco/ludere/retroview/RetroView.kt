package com.draco.ludere.retroview

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.draco.ludere.R
import com.draco.ludere.repositories.Storage
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.Variable
import io.reactivex.disposables.CompositeDisposable

class RetroView(private val context: Context, compositeDisposable: CompositeDisposable) {
    private val storage = Storage.getInstance(context)

    private val _frameRendered = MutableLiveData(false)
    val frameRendered: LiveData<Boolean> = _frameRendered

    private val retroViewData = GLRetroViewData(context).apply {
        coreFilePath = "libcore.so"
        gameFileBytes = context.resources.openRawResource(R.raw.rom).use { it.readBytes() }
        shader = GLRetroView.SHADER_SHARP
        variables = getCoreVariables()

        if (storage.sram.exists()) {
            storage.sram.inputStream().use {
                saveRAMState = it.readBytes()
            }
        }
    }

    val view = GLRetroView(context, retroViewData)

    init {
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        )
        params.gravity = Gravity.CENTER
        view.layoutParams = params

        val renderDisposable = view
            .getGLRetroEvents()
            .takeUntil { _frameRendered.value == true }
            .subscribe {
                if (it == GLRetroView.GLRetroEvents.FrameRendered && _frameRendered.value == false) {
                    _frameRendered.postValue(true)
                }
            }
        compositeDisposable.add(renderDisposable)
    }

    private fun getCoreVariables(): Array<Variable> {
        val variables = arrayListOf<Variable>()
        val rawVariablesString = context.getString(R.string.config_variables)
        val rawVariables = rawVariablesString.split(",")

        for (rawVariable in rawVariables) {
            val rawVariableSplit = rawVariable.split("=")
            if (rawVariableSplit.size != 2)
                continue

            variables.add(Variable(rawVariableSplit[0], rawVariableSplit[1]))
        }

        return variables.toTypedArray()
    }
}