package com.wocus.bmap

import android.app.Application
import com.baidu.mapapi.SDKInitializer

/**
 * Created by Administrator on 2018/8/15.
 */
class BaseApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        SDKInitializer.initialize(this)
    }
}