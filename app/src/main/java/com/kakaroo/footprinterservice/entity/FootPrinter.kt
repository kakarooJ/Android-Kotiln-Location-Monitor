package com.kakaroo.footprinterservice.entity

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
//칼럼 time 값은 날짜와 시간이 공백으로 구성된다 (ex> 2022-03-01 07:01:52)
data class FootPrinter(
    @SerializedName("id") val id: Long,
    @SerializedName("time") var time: String?,
    @SerializedName("latitude") var latitude: Double,
    @SerializedName("longitude") var longitude: Double
    ) : Parcelable


